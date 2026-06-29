package app.tinks.tink.leetkeeper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface LeetKeeperApi {
    @GET("leetkeeper/public-plans")
    suspend fun getPublicPlans(): List<LeetKeeperPublicPlanDto>

    @GET("leetkeeper/public-plans/{planId}")
    suspend fun getPublicPlanDetail(
        @Path("planId") planId: Int,
    ): LeetKeeperPlanDetailDto

    @GET("leetkeeper/ongoing-plan")
    suspend fun getOngoingPlan(): LeetKeeperOngoingPlanDto?

    @POST("leetkeeper/ongoing-plans")
    suspend fun createOngoingPlan(
        @Body payload: LeetKeeperStartPlanRequest,
    ): LeetKeeperOngoingPlanDto

    @PATCH("leetkeeper/ongoing-plans/{planId}")
    suspend fun updateOngoingPlan(
        @Path("planId") planId: String,
        @Body payload: LeetKeeperUpdateOngoingPlanRequest,
    ): LeetKeeperOngoingPlanDto

    @GET("leetkeeper/problems/{problemId}")
    suspend fun getProblemDetail(
        @Path("problemId") problemId: String,
    ): LeetKeeperProblemDetailDto

    @POST("leetkeeper/transactions")
    suspend fun createTransaction(
        @Body payload: LeetKeeperCreateTransactionRequest,
    ): LeetKeeperTransactionDto
}

@Serializable
data class LeetKeeperPublicPlanDto(
    val id: Int,
    val title: String,
    val introduction: String,
    val copy: Int = 0,
    @SerialName("total_problems")
    val totalProblems: Int? = null,
    @SerialName("sub_modules")
    val subModules: List<LeetKeeperModuleDto>? = null,
)

@Serializable
data class LeetKeeperPlanDetailDto(
    val id: Int,
    val title: String,
    val introduction: String,
    val copy: Int = 0,
    @SerialName("total_problems")
    val totalProblems: Int,
    @SerialName("sub_modules")
    val subModules: List<LeetKeeperModuleDto>,
)

@Serializable
data class LeetKeeperOngoingPlanDto(
    val id: String,
    val title: String,
    val introduction: String,
    val language: String,
    val dones: List<Int> = emptyList(),
    @SerialName("total_problems")
    val totalProblems: Int,
    val progress: Int,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("sub_modules")
    val subModules: List<LeetKeeperModuleDto>,
)

@Serializable
data class LeetKeeperModuleDto(
    val name: String,
    @SerialName("total_problems")
    val totalProblems: Int,
    val progress: Int = 0,
    val problems: List<LeetKeeperProblemSummaryDto>,
)

@Serializable
data class LeetKeeperProblemSummaryDto(
    val id: String,
    val title: String,
    val difficulty: Int,
    val done: Boolean = false,
    val link: String? = null,
)

@Serializable
data class LeetKeeperProblemDetailDto(
    val id: String,
    val title: String,
    val details: String,
    val difficulty: Int,
    val link: String? = null,
    val transactions: List<LeetKeeperTransactionDto> = emptyList(),
)

@Serializable
data class LeetKeeperTransactionDto(
    val id: String? = null,
    val language: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("problem_id")
    val problemId: String,
    @SerialName("problem_link")
    val problemLink: String? = null,
    val time: Int? = null,
    val submission: String? = null,
)

@Serializable
data class LeetKeeperStartPlanRequest(
    @SerialName("public_plan_id")
    val publicPlanId: Int,
    val language: String,
)

@Serializable
data class LeetKeeperUpdateOngoingPlanRequest(
    val dones: List<Int>,
)

@Serializable
data class LeetKeeperCreateTransactionRequest(
    val language: String,
    @SerialName("problem_id")
    val problemId: String,
    @SerialName("problem_link")
    val problemLink: String? = null,
    val time: Int,
    val submission: String? = null,
)
