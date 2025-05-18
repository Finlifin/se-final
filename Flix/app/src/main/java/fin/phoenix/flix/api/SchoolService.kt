package fin.phoenix.flix.api

import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.School
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 学校相关API接口
 */
interface SchoolService {
    /**
     * 获取所有学校列表
     */
    @GET("schools")
    suspend fun getSchools(): Response<GenericApiResponse<List<School>>>
    
    /**
     * 根据学校ID获取校区列表
     */
    @GET("schools/{schoolId}/campuses")
    suspend fun getCampusesBySchoolId(
        @Path("schoolId") schoolId: String
    ): Response<GenericApiResponse<List<Campus>>>
    
    /**
     * 搜索学校
     */
    @GET("schools/search")
    suspend fun searchSchools(
        @Query("query") query: String
    ): Response<GenericApiResponse<List<School>>>
    
    /**
     * 获取学校详情
     */
    @GET("schools/{schoolId}")
    suspend fun getSchool(
        @Path("schoolId") schoolId: String
    ): Response<GenericApiResponse<School>>
    
    /**
     * 获取校区详情
     */
    @GET("campuses/{campusId}")
    suspend fun getCampus(
        @Path("campusId") campusId: String
    ): Response<GenericApiResponse<Campus>>
    
    /**
     * 创建学校
     */
    @POST("schools")
    suspend fun createSchool(
        @Body params: Map<String, @JvmSuppressWildcards Any?>
    ): Response<GenericApiResponse<School>>
    
    /**
     * 更新学校
     */
    @PUT("schools/{schoolId}")
    suspend fun updateSchool(
        @Path("schoolId") schoolId: String,
        @Body params: Map<String, Any?>
    ): Response<GenericApiResponse<School>>
    
    /**
     * 删除学校
     */
    @DELETE("schools/{schoolId}")
    suspend fun deleteSchool(
        @Path("schoolId") schoolId: String
    ): Response<GenericApiResponse<Unit>>
    
    /**
     * 创建校区
     */
    @POST("campuses")
    suspend fun createCampus(
        @Body params: @JvmSuppressWildcards Map<String, Any?>
    ): Response<GenericApiResponse<Campus>>
    
    /**
     * 更新校区
     */
    @PUT("campuses/{campusId}")
    suspend fun updateCampus(
        @Path("campusId") campusId: String,
        @Body params: Map<String, Any?>
    ): Response<GenericApiResponse<Campus>>
    
    /**
     * 删除校区
     */
    @DELETE("campuses/{campusId}")
    suspend fun deleteCampus(
        @Path("campusId") campusId: String
    ): Response<GenericApiResponse<Unit>>
}