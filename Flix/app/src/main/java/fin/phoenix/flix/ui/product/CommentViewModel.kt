package fin.phoenix.flix.ui.product

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import fin.phoenix.flix.data.Comment
import fin.phoenix.flix.data.CommentContext
import fin.phoenix.flix.repository.CommentRepository
import fin.phoenix.flix.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    
    private val commentRepository = CommentRepository(application)
    
    private val _selectedComment = MutableLiveData<Comment?>(null)
    val selectedComment: LiveData<Comment?> = _selectedComment
    
    private val _commentContext = MutableLiveData<Resource<CommentContext>>(Resource.Loading)
    val commentContext: LiveData<Resource<CommentContext>> = _commentContext
    
    private val _newCommentContent = MutableLiveData<String>("")
    val newCommentContent: LiveData<String> = _newCommentContent
    
    private val _newReplyContent = MutableLiveData<String>("")
    val newReplyContent: LiveData<String> = _newReplyContent
    
    private val _isSubmitting = MutableLiveData<Boolean>(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    // 缓存不同productId对应的评论分页流
    private val commentFlows = mutableMapOf<String, Flow<PagingData<Comment>>>()
    
    // 标记已初始化的商品ID，避免重复加载
    private val initializedProductIds = mutableSetOf<String>()
    
    // 刷新商品评论，确保只加载一次
    fun refreshProductComments(productId: String) {
        if (!initializedProductIds.contains(productId)) {
            initializedProductIds.add(productId)
            getProductComments(productId)
        }
    }
    
    // 获取商品评论的Paging数据流，使用缓存避免重复创建
    fun getProductComments(productId: String): Flow<PagingData<Comment>> {
        return commentFlows.getOrPut(productId) {
            Pager(
                config = PagingConfig(
                    pageSize = 10,
                    enablePlaceholders = false,
                    initialLoadSize = 10
                ),
                pagingSourceFactory = {
                    CommentPagingSource(commentRepository, productId)
                }
            ).flow.cachedIn(viewModelScope)
        }
    }
    
    // 选择评论，加载评论上下文（根评论及其所有回复）
    fun selectComment(comment: Comment) {
        _selectedComment.value = comment
        loadCommentContext(comment.id)
    }
    
    // 清除选择的评论
    fun clearSelectedComment() {
        _selectedComment.value = null
        _commentContext.value = Resource.Loading
    }
    
    // 更新新评论内容
    fun updateNewCommentContent(content: String) {
        _newCommentContent.value = content
    }
    
    // 更新新回复内容
    fun updateNewReplyContent(content: String) {
        _newReplyContent.value = content
    }
    
    // 加载评论上下文（评论及其所有回复）
    fun loadCommentContext(commentId: String) {
        viewModelScope.launch {
            _commentContext.value = Resource.Loading
            _commentContext.value = commentRepository.getCommentContext(commentId)
        }
    }
    
    // 提交新评论
    fun submitComment(productId: String, onSuccess: () -> Unit) {
        val content = _newCommentContent.value
        
        if (content.isNullOrBlank()) return
        
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = commentRepository.createComment(productId, content)
            
            if (result is Resource.Success) {
                _newCommentContent.value = ""
                // 清除该商品的评论缓存，确保下次获取到最新评论
                commentFlows.remove(productId)
                onSuccess()
            }
            
            _isSubmitting.value = false
        }
    }
    
    // 提交评论回复
    fun submitReply(commentId: String, onSuccess: () -> Unit) {
        val content = _newReplyContent.value
        
        if (content.isNullOrBlank()) return
        
        viewModelScope.launch {
            _isSubmitting.value = true
            val result = commentRepository.replyToComment(commentId, content)
            
            if (result is Resource.Success) {
                _newReplyContent.value = ""
                // 如果回复成功，重新加载评论上下文
                loadCommentContext(commentId)
                // 这里可能需要清除相关商品的缓存，如果有上下文的话
                val comment = _selectedComment.value
                if (comment != null) {
                    commentFlows.remove(comment.productId)
                }
                onSuccess()
            }
            
            _isSubmitting.value = false
        }
    }
    
    // 点赞评论
    fun likeComment(comment: Comment) {
        viewModelScope.launch {
            val result = commentRepository.likeComment(comment.id)
            if (result is Resource.Success) {
                // 更新评论状态
                comment.isLiked = true
                comment.likesCount += 1
                
                // 如果是当前选中的评论，也更新评论上下文
                if (_selectedComment.value?.id == comment.id) {
                    _selectedComment.value = comment
                    _commentContext.value?.let { context ->
                        if (context is Resource.Success) {
                            val updatedComments = context.data.replies.map {
                                if (it.id == comment.id) comment else it
                            }
                            _commentContext.value = Resource.Success(context.data.copy(replies = updatedComments))
                        }
                    }
                }
            }
        }
    }
    
    // 取消点赞评论
    fun unlikeComment(comment: Comment) {
        viewModelScope.launch {
            val result = commentRepository.unlikeComment(comment.id)
            if (result is Resource.Success) {
                // 更新评论状态
                comment.isLiked = false
                comment.likesCount = (comment.likesCount - 1).coerceAtLeast(0)
                
                // 如果是当前选中的评论，也更新评论上下文
                if (_selectedComment.value?.id == comment.id) {
                    _selectedComment.value = comment
                    _commentContext.value?.let { context ->
                        if (context is Resource.Success) {
                            val updatedComments = context.data.replies.map {
                                if (it.id == comment.id) comment else it
                            }
                            _commentContext.value = Resource.Success(context.data.copy(replies = updatedComments))
                        }
                    }
                }
            }
        }
    }
    
    // 刷新特定商品的评论（强制重新加载）
    fun forceRefreshProductComments(productId: String) {
        commentFlows.remove(productId)
        initializedProductIds.remove(productId)
    }

    // 添加清理方法，在退出页面时调用
    fun clearProductResources(productId: String) {
        commentFlows.remove(productId)
        initializedProductIds.remove(productId)
        if (_selectedComment.value?.productId == productId) {
            clearSelectedComment()
        }
    }
        
    // 重写 onCleared 方法，在 ViewModel 被销毁时清理资源
    override fun onCleared() {
        super.onCleared()
        // 清空所有缓存
        commentFlows.clear()
    }
}