#!/usr/bin/env python3
import argparse
import json
import re
import sqlite3
from pathlib import Path

from pypdf import PdfReader


def parse_pdf(pdf_path: Path):
    text = "\n".join(page.extract_text() or "" for page in PdfReader(str(pdf_path)).pages)
    text = re.sub(r"IT Certification Guaranteed, The Easy Way!\s*\n\d+\s*\n", "\n", text)
    parts = re.split(r"(?=\bNO\.\s*\d+\b)", text)
    questions = []

    for part in parts[1:]:
        id_match = re.match(r"NO\.\s*(\d+)\s+", part)
        if not id_match:
            raise ValueError(f"Question id missing near: {part[:80]!r}")
        question_id = int(id_match.group(1))
        answer_match = re.search(r"(?im)^Answer:\s*([^\n]+)", part)
        if not answer_match:
            raise ValueError(f"Answer missing for question {question_id}")

        before_answer = part[: answer_match.start()].strip()
        after_answer = part[answer_match.end() :].strip()
        explanation = None
        explanation_match = re.search(r"(?is)^Explanation:\s*(.*)$", after_answer)
        if explanation_match:
            explanation = clean_text(explanation_match.group(1))

        body = re.sub(r"^NO\.\s*\d+\s+", "", before_answer, count=1).strip()
        option_matches = list(re.finditer(r"(?:^|\n|\s)([A-F])\.\s+", body))
        labels = [match.group(1) for match in option_matches]
        if "A" not in labels:
            raise ValueError(f"Question {question_id} is missing option A")
        option_matches = option_matches[labels.index("A") :]
        prompt = clean_text(body[: option_matches[0].start()])

        choices = []
        for index, match in enumerate(option_matches):
            label = match.group(1)
            start = match.end()
            end = option_matches[index + 1].start() if index + 1 < len(option_matches) else len(body)
            choices.append({"label": label, "text": clean_text(body[start:end])})

        answer_labels = sorted(answer_match.group(1).strip().split())
        choice_labels = {choice["label"] for choice in choices}
        if any(label not in choice_labels for label in answer_labels):
            raise ValueError(f"Question {question_id} answer does not map to choices")

        questions.append(
            {
                "id": question_id,
                "prompt": prompt,
                "answer_labels": answer_labels,
                "explanation": explanation,
                "choices": choices,
            }
        )

    ids = [question["id"] for question in questions]
    expected_ids = list(range(1, len(questions) + 1))
    if ids != expected_ids:
        raise ValueError(f"Question ids are not sequential: {ids[:5]} ... {ids[-5:]}")
    return questions


def clean_text(value: str) -> str:
    text = re.sub(r"\s+", " ", value).strip()
    text = re.sub(r"\s+'\s*", "'", text)
    text = re.sub(r'\s+"\s*', '"', text)
    text = re.sub(r"\s+([,.;:!?])", r"\1", text)
    text = re.sub(r"([(\[])\s+", r"\1", text)
    text = re.sub(r"\s+([)\]])", r"\1", text)
    text = re.sub(r"(?<=[A-Za-z)])\d{1,3}(?=(?:\s|[A-Z.,;:)]|$))", "", text)
    text = re.sub(r"\s+\d{1,3}$", "", text)
    return text


def update_database(db_path: Path, questions):
    con = sqlite3.connect(db_path)
    try:
        con.execute("PRAGMA foreign_keys = ON")
        con.executescript(
            """
            CREATE TABLE IF NOT EXISTS salesforce_question (
              id INTEGER NOT NULL PRIMARY KEY,
              prompt TEXT NOT NULL,
              answer_labels TEXT NOT NULL,
              explanation TEXT
            );
            CREATE TABLE IF NOT EXISTS salesforce_choice (
              id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
              question_id INTEGER NOT NULL,
              label TEXT NOT NULL,
              text TEXT NOT NULL,
              FOREIGN KEY(question_id) REFERENCES salesforce_question(id) ON DELETE CASCADE
            );
            CREATE UNIQUE INDEX IF NOT EXISTS index_salesforce_choice_question_id_label
              ON salesforce_choice(question_id, label);
            CREATE INDEX IF NOT EXISTS index_salesforce_choice_question_id
              ON salesforce_choice(question_id);
            CREATE TABLE IF NOT EXISTS salesforce_local_progress (
              question_id INTEGER NOT NULL PRIMARY KEY,
              done INTEGER NOT NULL DEFAULT 0,
              correct_count INTEGER NOT NULL DEFAULT 0,
              incorrect_count INTEGER NOT NULL DEFAULT 0,
              last_answered_at TEXT
            );
            CREATE TABLE IF NOT EXISTS salesforce_answer_event_queue (
              id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
              session_id TEXT NOT NULL,
              session_type TEXT NOT NULL,
              question_id INTEGER NOT NULL,
              selected_labels TEXT NOT NULL,
              correct_labels TEXT NOT NULL,
              correct INTEGER NOT NULL,
              answered_at TEXT NOT NULL,
              started_at TEXT,
              finished_at TEXT
            );
            CREATE INDEX IF NOT EXISTS index_salesforce_answer_event_queue_session_id
              ON salesforce_answer_event_queue(session_id);
            """
        )
        con.execute("DELETE FROM salesforce_choice")
        con.execute("DELETE FROM salesforce_question")
        for question in questions:
            con.execute(
                "INSERT INTO salesforce_question(id, prompt, answer_labels, explanation) VALUES (?, ?, ?, ?)",
                (
                    question["id"],
                    question["prompt"],
                    ",".join(question["answer_labels"]),
                    question["explanation"],
                ),
            )
            for choice in question["choices"]:
                con.execute(
                    "INSERT INTO salesforce_choice(question_id, label, text) VALUES (?, ?, ?)",
                    (question["id"], choice["label"], choice["text"]),
                )
        con.execute("PRAGMA user_version = 3")
        con.commit()
    finally:
        con.close()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pdf", required=True, type=Path)
    parser.add_argument("--db", required=True, type=Path)
    parser.add_argument("--json", required=True, type=Path)
    args = parser.parse_args()

    questions = parse_pdf(args.pdf)
    if len(questions) != 153:
        raise ValueError(f"Expected 153 questions, got {len(questions)}")
    multi_answer_count = sum(1 for question in questions if len(question["answer_labels"]) > 1)
    if multi_answer_count != 19:
        raise ValueError(f"Expected 19 multi-answer questions, got {multi_answer_count}")

    args.json.parent.mkdir(parents=True, exist_ok=True)
    args.json.write_text(
        json.dumps({"questions": questions}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    update_database(args.db, questions)
    print(f"Generated {len(questions)} Salesforce questions")


if __name__ == "__main__":
    main()
