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
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ProductAbstract> {
        return try {
            // 页码从1开始，如果是初始加载则默认为1
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
                        // 调用Product类的toAbstract()方法，而不是toProductAbstract()
                        it.toAbstract()
                    }
                    
                    // 计算下一页和上一页
                    val nextKey = if (products.size < params.loadSize) null else page + 1
                    val prevKey = if (page > 1) page - 1 else null
                    
                    LoadResult.Page(
                        data = products,
                        prevKey = prevKey,
                        nextKey = nextKey
                    )
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