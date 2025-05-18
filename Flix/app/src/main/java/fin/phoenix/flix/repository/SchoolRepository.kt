package fin.phoenix.flix.repository

import android.content.Context
import fin.phoenix.flix.api.RetrofitClient
import fin.phoenix.flix.api.SchoolService
import fin.phoenix.flix.data.Campus
import fin.phoenix.flix.data.School
import fin.phoenix.flix.util.Resource
import fin.phoenix.flix.util.toResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 学校仓库类，处理与学校和校区相关的业务逻辑
 */
class SchoolRepository(context: Context) {

    private val schoolService = RetrofitClient.createService(SchoolService::class.java, context)

    /**
     * 获取所有学校列表
     */
    suspend fun getSchools(): Resource<List<School>> = withContext(Dispatchers.IO) {
        schoolService.getSchools().toResource("获取学校列表失败")
    }

    /**
     * 根据学校ID获取校区列表
     */
    suspend fun getCampusesBySchoolId(schoolId: String): Resource<List<Campus>> =
        withContext(Dispatchers.IO) {
            schoolService.getCampusesBySchoolId(schoolId).toResource("获取校区列表失败")
        }

    /**
     * 搜索学校
     */
    suspend fun searchSchools(query: String): Resource<List<School>> = withContext(Dispatchers.IO) {
        schoolService.searchSchools(query).toResource("搜索学校失败")
    }

    /**
     * 获取学校详情
     */
    suspend fun getSchool(schoolId: String): Resource<School> = withContext(Dispatchers.IO) {
        schoolService.getSchool(schoolId).toResource("获取学校详情失败")
    }

    /**
     * 获取校区详情
     */
    suspend fun getCampus(campusId: String): Resource<Campus> =
        withContext(Dispatchers.IO) {
            schoolService.getCampus(campusId).toResource("获取校区详情失败")
        }

    /**
     * 创建学校
     * @param name 学校名称
     * @param location 学校位置
     * @param description 学校描述
     * @param logoUrl 学校logo URL（可选）
     */
    suspend fun createSchool(
        name: String
    ): Resource<School> = withContext(Dispatchers.IO) {
        val params = mapOf(
            "name" to name,
        )

        schoolService.createSchool(params).toResource("创建学校失败")
    }

    /**
     * 更新学校信息
     * @param schoolId 学校ID
     * @param name 学校名称（可选）
     * @param location 学校位置（可选）
     * @param description 学校描述（可选）
     * @param logoUrl 学校logo URL（可选）
     */
    suspend fun updateSchool(
        schoolId: String,
        name: String? = null,
        location: String? = null,
        description: String? = null,
        logoUrl: String? = null
    ): Resource<School> = withContext(Dispatchers.IO) {
        val params = mapOf(
            "name" to name,
            "location" to location,
            "description" to description,
            "logo_url" to logoUrl
        ).filterValues { it != null }

        schoolService.updateSchool(schoolId, params).toResource("更新学校失败")
    }

    /**
     * 删除学校
     * @param schoolId 要删除的学校ID
     */
    suspend fun deleteSchool(schoolId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        schoolService.deleteSchool(schoolId).toResource("删除学校失败")
    }

    /**
     * 创建校区
     * @param schoolId 学校ID
     * @param name 校区名称
     * @param location 校区位置
     * @param description 校区描述（可选）
     * @param imageUrl 校区图片URL（可选）
     */
    suspend fun createCampus(
        schoolId: String,
        name: String,
        location: String,
        description: String? = null,
        imageUrl: String? = null
    ): Resource<Campus> = withContext(Dispatchers.IO) {
        val params = mapOf(
            "school_id" to schoolId,
            "name" to name,
            "address" to location,
            "description" to description,
            "image_url" to imageUrl
        ).filterValues { it != null }

        schoolService.createCampus(params).toResource("创建校区失败")
    }

    /**
     * 更新校区信息
     * @param campusId 校区ID
     * @param name 校区名称（可选）
     * @param location 校区位置（可选）
     * @param description 校区描述（可选）
     * @param imageUrl 校区图片URL（可选）
     */
    suspend fun updateCampus(
        campusId: String,
        name: String? = null,
        location: String? = null,
        description: String? = null,
        imageUrl: String? = null
    ): Resource<Campus> = withContext(Dispatchers.IO) {
        val params = mapOf(
            "name" to name,
            "location" to location,
            "description" to description,
            "image_url" to imageUrl
        ).filterValues { it != null }

        schoolService.updateCampus(campusId, params).toResource("更新校区失败")
    }

    /**
     * 删除校区
     * @param campusId 要删除的校区ID
     */
    suspend fun deleteCampus(campusId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        schoolService.deleteCampus(campusId).toResource("删除校区失败")
    }

    /**
     * 获取所有校区列表
     * 注意：此功能在现有的SchoolService接口中不存在，如有需要可以添加
     */
    suspend fun getAllCampuses(): Resource<List<Campus>> = withContext(Dispatchers.IO) {
        // 首先获取所有学校，然后从每个学校中获取校区
        val schoolsResponse = getSchools()

        if (schoolsResponse is Resource.Success) {
            val schools = schoolsResponse.data
            val allCampuses = mutableListOf<Campus>()

            // 遍历每个学校获取其校区
            schools.forEach { school ->
                val campusesResponse = getCampusesBySchoolId(school.id)
                if (campusesResponse is Resource.Success) {
                    allCampuses.addAll(campusesResponse.data)
                }
            }

            Resource.Success(allCampuses)
        } else schoolsResponse as? Resource.Error ?: Resource.Error("获取所有校区失败")
    }
}