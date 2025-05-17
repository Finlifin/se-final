package fin.phoenix.flix.ui.home

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fin.phoenix.flix.data.ProductAbstract
import fin.phoenix.flix.repository.ProductRepository
import fin.phoenix.flix.util.Resource

class ProductPagingSource(
    private val repository: ProductRepository,
    private val category: String? = null,
    private val searchQuery: String? = null,
    private val sortBy: String? = "postTime",
    private val sortOrder: String? = "desc"
) : PagingSource<Int, ProductAbstract>() {
    
    // 允许分页系统重用键值，避免同一页被多次请求时的错误
    override val keyReuseSupported: Boolean = true
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProductAbstract> {
        return try {
            val page = params.key ?: 1
            
            // 调用repository获取数据
            val result = repository.getProducts(
                page = page,
                limit = params.loadSize,
                category = if (category == "全部") null else category,
                searchQuery = searchQuery,
                sortBy = sortBy,
                sortOrder = sortOrder
            )
            
            when (result) {
                is Resource.Success -> {
                    val products = result.data.products.map { 
                        // 调用Product类的toAbstract()方法
                        it.toAbstract()
                    }
                    
                    // 使用服务器返回的总页数信息，但使用请求的页码（而不是服务器返回的currentPage）
                    val totalPages = result.data.totalPages 
                    
                    // 只有当当前请求页小于总页数时，才返回下一页的页码
                    val nextKey = if (page < totalPages) page + 1 else null
                    val prevKey = if (page > 1) page - 1 else null
                    
                    // 如果返回的数据为空，即使页码有效也不再请求下一页
                    if (products.isEmpty()) {
                        LoadResult.Page(
                            data = products,
                            prevKey = prevKey,
                            nextKey = null // 确保空结果不会继续请求
                        )
                    } else {
                        LoadResult.Page(
                            data = products,
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    }
                }
                is Resource.Error -> {
                    LoadResult.Error(Exception(result.message))
                }
                is Resource.Loading -> {
                    LoadResult.Error(Exception("加载中..."))
                }
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
    
    override fun getRefreshKey(state: PagingState<Int, ProductAbstract>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}