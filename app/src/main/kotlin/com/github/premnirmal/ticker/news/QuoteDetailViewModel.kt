package com.github.premnirmal.ticker.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuoteDetailViewModel(application: Application): AndroidViewModel(application) {

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var newsProvider: NewsProvider
  @Inject internal lateinit var historyProvider: IHistoryProvider
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  private val _quote = MutableLiveData<Quote>()
  val quote: LiveData<Quote>
    get() = _quote
  private val _data = MutableLiveData<List<DataPoint>>()
  val data: LiveData<List<DataPoint>>
    get() = _data
  private val _dataFetchError = MutableLiveData<FetchException>()
  val dataFetchError: LiveData<FetchException>
    get() = _dataFetchError
  private val _newsData = MutableLiveData<List<NewsArticle>>()
  val newsData: LiveData<List<NewsArticle>>
    get() = _newsData
  private val _newsError = MutableLiveData<FetchException>()
  val newsError: LiveData<FetchException>
    get() = _newsError
  private val _unauthorized = MutableLiveData<Nothing>()
  val unauthorized: LiveData<Nothing>
    get() = _unauthorized

  init {
    Injector.appComponent.inject(this)
  }

  fun fetchQuote(ticker: String) {
    viewModelScope.launch {
      if (_quote.value != null) {
        _quote.postValue(_quote.value)
        return@launch
      }
      val fetchStock = stocksProvider.fetchStock(ticker)
      if (fetchStock.wasSuccessful) {
        _quote.value = fetchStock.data
      }
    }
  }

  fun isInPortfolio(ticker: String): Boolean {
    return stocksProvider.hasTicker(ticker)
  }

  fun removeStock(ticker: String) {
    stocksProvider.removeStock(ticker)
  }

  fun fetchHistoricalDataShort(symbol: String) {
    viewModelScope.launch {
      if (_data.value != null) {
        _data.postValue(_data.value)
        return@launch
      }
      val result = historyProvider.getHistoricalDataShort(symbol)
      if (result.wasSuccessful) {
        _data.value = result.data
      } else {
        _dataFetchError.postValue(result.error)
      }
    }
  }

  fun hasTicker(ticker: String): Boolean {
    return stocksProvider.hasTicker(ticker)
  }

  fun fetchNews(quote: Quote) {
    viewModelScope.launch {
      if (_newsData.value != null) {
        _newsData.postValue(_newsData.value)
        return@launch
      }
      val query = quote.newsQuery()
      val result = newsProvider.getNews(query)
      when {
        result.wasSuccessful -> {
          _newsData.value = result.data
        }
        result.unauthorized -> {
          _unauthorized.value = null
        }
        else -> {
          _newsError.value = result.error
        }
      }
    }
  }
}