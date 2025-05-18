package fin.phoenix.flix.ui.product

import androidx.paging.PagingSource
import androidx.paging.PagingState
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.repository.CommentRepository
import fin.phoenix.flix.util.Resource

/**
 * 评论分页数据源，用于商品详情页加载评论
 */
class CommentPagingSource(
    private val repository: CommentRepository,
    private val productId: String
) : PagingSource<Int, Comment>() {
    
    override fun getRefreshKey(state: PagingState<Int, Comment>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
    
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Comment> {
        try {
            // 如果是第一次加载，页码为1，否则使用传入的页码
            val page = params.key ?: 1
            val limit = params.loadSize
            
            val result = repository.getProductComments(
                productId = productId,
                page = page,
                limit = limit
            )
            
            return when (result) {
                is Resource.Success -> {
                    val comments = result.data
                    
                    // 计算前一页和下一页的页码
                    val nextKey = if (comments.isEmpty()) null else page + 1
                    val prevKey = if (page > 1) page - 1 else null
                    
                    LoadResult.Page(
                        data = comments,
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
            return LoadResult.Error(e)
        }
    }
}