/**
 *	@copyright 亿康通-2015
 * 	@author wanruome
 * 	@create 2015年11月4日 下午2:09:21
 */
package com.ruomm.base.http.asynchttp;

import java.io.File;
import java.util.HashMap;

import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.ruomm.base.http.HttpConfig;
import com.ruomm.base.http.config.DataHttpListener;
import com.ruomm.base.http.config.ResponseData;
import com.ruomm.base.http.config.TextCacheGetListener;
import com.ruomm.base.http.config.TextCacheSaveListener;
import com.ruomm.base.tools.Base64;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import cz.msebera.android.httpclient.Header;

/**
 * 使用Async-Http-Client的网络请求类
 * <p>
 * 默认使用post模式的异步下载
 *
 * @author Ruby
 */
public final class DataAsyncHttp {
	// 网络请求的Json回调监听
	private DataHttpListener dataHttpListener;
	// 网络请求的Json回调目标解析对象的类型
	private Class<?> cls;
	// 网络请求返回自动解析的数据类型
	// private HttpConfig.RESPONSETYPE mRspType = null;
	// 网络请求的时候调试输出的tag
	private String debugTag;
	// 是否使用异步模式进行网络请求
	private boolean isAjax;
	// 是否使用Post模式设置参数进行网络请求
	private boolean isPost;
	// 是否输出调试信息
	private boolean isDebug;
	// 网络请求的路径
	private String Url;
	// 判定请求参数是否正确
	private boolean isTrueParams = true;
	// 网络请求的参数列表
	private final HashMap<String, Object> mObjectMap = new HashMap<String, Object>();
	private String tag;
	// 同步网络请求结果
	private ResponseData mResponseData;
	// 请求客户端
	private RequestHandle mRequestHandle = null;
	// 下载请求的key值，构建模式为参数+路经通过MD5加密后的值
	private String key;
	// 是否把缓存的结果调用JsonHttpListener的httpCallBack方法来统一处理
	private boolean isCacheExecuteToHttp = true;
	// 网络请求前获取缓存数据，可依据缓存结果是否继续网络请求
	private TextCacheGetListener cacheGetListener;
	// 网络请求结束后存储请求结果到缓存数据
	private TextCacheSaveListener cacheSaveListener;

	/**
	 * 构造方法
	 *
	 * @return
	 */
	public DataAsyncHttp() {
		this.Url = null;
		this.isAjax = true;
		this.isPost = true;
		if (HttpConfig.debug_autonewhttp) {
			setDebug();
		}

	}

	/**
	 * @param cacheGetListener
	 *            缓存读取设置
	 * @return
	 */
	public DataAsyncHttp setCacheGetListener(TextCacheGetListener cacheGetListener) {
		this.cacheGetListener = cacheGetListener;
		return this;
	}

	/**
	 * 是否把缓存的结果调用JsonHttpListener的httpCallBack方法来统一处理
	 *
	 * @param isCacheExecuteToHttp
	 *            是否调用JsonHttpListener的httpCallBack方法来统一处理缓存结果
	 * @return
	 */
	public DataAsyncHttp setCacheExecuteToHttp(boolean isCacheExecuteToHttp) {
		this.isCacheExecuteToHttp = isCacheExecuteToHttp;
		return this;
	}

	/**
	 * @param cacheSaveListener
	 *            缓存写入设置
	 * @return
	 */
	public DataAsyncHttp setCacheSaveListener(TextCacheSaveListener cacheSaveListener) {
		this.cacheSaveListener = cacheSaveListener;
		return this;
	}

	/**
	 * @param listener
	 *            Http请求监听
	 * @return
	 */
	public DataAsyncHttp setHttpListener(DataHttpListener listener) {
		this.dataHttpListener = listener;
		return this;
	}

	/**
	 * 调试模式设置
	 *
	 * @return
	 */
	public DataAsyncHttp setDebug() {
		if (HttpConfig.debug_public) {
			this.debugTag = "AsyncHttp";
			this.isDebug = true;
		}
		else {
			this.debugTag = null;
			this.isDebug = false;
		}

		return this;
	}

	/**
	 * 调试模式Tag设置
	 *
	 * @param debugTag
	 *            调试模式输出信息的Tag
	 * @return
	 */
	public DataAsyncHttp setDebug(String debugTag) {

		if (HttpConfig.debug_public) {
			if (TextUtils.isEmpty(debugTag)) {
				this.debugTag = "AsyncHttp";
			}
			else {
				this.debugTag = debugTag;
			}
			this.isDebug = true;
		}
		else {
			this.debugTag = null;
			this.isDebug = false;
		}

		return this;
	}

	/**
	 * @param url
	 *            请求路径设置
	 * @return
	 */

	public DataAsyncHttp setUrl(String url) {
		this.Url = url;
		return this;
	}

	/**
	 * 请求参数设置
	 *
	 * @param hashMap
	 * @return
	 */
	public DataAsyncHttp setRequestParams(HashMap<String, String> hashMap) {
		// this.mStringMap = hashMap;
		if (null != hashMap && !hashMap.isEmpty()) {
			mObjectMap.putAll(hashMap);
		}
		return this;
	}

	public DataAsyncHttp setRequestParamsFile(HashMap<String, File> fileMap) {
		// this.mFileMap = fileMap;
		// return this;
		if (null != fileMap && !fileMap.isEmpty()) {
			mObjectMap.putAll(fileMap);
		}
		return this;
	}

	public DataAsyncHttp setRequestParamsFileArray(HashMap<String, File[]> fileArrayMap) {
		// this.mFileArrayMap = fileArrayMap;
		// return this;
		if (null != fileArrayMap && !fileArrayMap.isEmpty()) {
			mObjectMap.putAll(fileArrayMap);
		}
		return this;
	}

	public DataAsyncHttp setRequestParamsObject(HashMap<String, Object> objectMap) {
		if (null != objectMap && !objectMap.isEmpty()) {
			mObjectMap.putAll(objectMap);
		}
		return this;
	}

	/**
	 * @param isPost
	 *            是Post请求还是Get请求
	 * @param isAjax
	 *            是否异步请求
	 * @return
	 */
	public DataAsyncHttp setMode(boolean isPost, boolean isAjax) {
		this.isPost = isPost;
		this.isAjax = isAjax;
		return this;
	}

	/**
	 * @param isAjax
	 *            是否异步请求
	 * @return
	 */
	public DataAsyncHttp setAjax(boolean isAjax) {
		this.isAjax = isAjax;
		return this;
	}

	/**
	 * @param isPost
	 *            是Post请求还是Get请求
	 * @return
	 */
	public DataAsyncHttp setMethodType(boolean isPost) {
		this.isPost = isPost;
		return this;
	}

	// /**
	// * 网络请求数据自动解析类型，默认JSON解析
	// *
	// * @param mType
	// * @return
	// */
	// public DataAsyncHttp setResponseType(HttpConfig.RESPONSETYPE mRspType) {
	// this.mRspType = mRspType;
	// return this;
	// }

	/**
	 * 获取请求tag标识
	 *
	 * @return
	 */
	public DataAsyncHttp setTag(String tag) {
		this.tag = tag;
		return this;
	}

	/**
	 * 获取请求tag标识
	 *
	 */
	public String getTag() {
		return this.tag;
	}

	/**
	 * 取消Http请求
	 */
	public void cancleCall() {
		if (null != mRequestHandle) {
			mRequestHandle.cancel(true);
		}
	}

	/**
	 * 获取请求的key值
	 *
	 * @return
	 */
	public String getAsyncHttpKey() {
		if (null == this.key) {
			this.key = AsyncHttpConfig.getKeyString(this.Url + this.tag, mObjectMap);
		}
		return this.key;
	}

	/**
	 * 判定Url是否合法
	 *
	 * @return
	 */
	private boolean isTrueHttp() {
		if (TextUtils.isEmpty(this.Url)) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * 判定是否启动HttpCient
	 *
	 * @return
	 */
	private boolean isEnableHttp() {
		if (isAjax) {
			if (null != AsyncHttpConfig.getClientAsync()) {
				return true;
			}
			else {
				if (TextUtils.isEmpty(debugTag)) {
					Log.e("AsyncHttp", "Client空错误@" + "异步Http请求Client为空值");
				}
				else {
					Log.e(debugTag, "Client空错误@" + "异步Http请求Client为空值");
				}

				return false;
			}
		}
		else {
			if (null != AsyncHttpConfig.getClientSync()) {
				return true;
			}
			else {
				if (TextUtils.isEmpty(debugTag)) {
					Log.e("AsyncHttp", "Client空错误@@" + "同步Http请求Client为空值");
				}
				else {
					Log.e(debugTag, "Client空错误@@" + "同步Http请求Client为空值");
				}
				return false;
			}
		}

	}

	/**
	 * 网络请求过滤器回调，若是返回true，则不执行httpCallBack方法了
	 *
	 * @param responseData
	 * @return
	 */
	private boolean httpCallBackFilter(byte[] responseData) {
		boolean isFilter = false;
		if (null != dataHttpListener) {
			isFilter = dataHttpListener.httpCallBackFilter(responseData, HttpConfig.Success);
		}
		if (isFilter) {
			if (isDebug) {
				Log.i(debugTag, "结果状态过滤拦截@" + "status:" + HttpConfig.Success_Filter + ";msg:过滤器拦截成功");
			}
		}
		return isFilter;
	}

	/**
	 * 回调网络请求结果
	 *
	 * @param resultObject
	 *            请求结果解析成的Object对象
	 * @param resultData
	 *            请求结果的原始数据
	 * @param status
	 *            请求结果的状态
	 */
	private void httpCallBack(final Object resultObject, final byte[] resultData, final int status) {
		if (!isAjax) {
			mResponseData = new ResponseData();
			mResponseData.resultObject = resultObject;
			mResponseData.status = status;
			mResponseData.resultData = resultData;
		}
		if (null == dataHttpListener) {
			if (isDebug) {
				Log.i(debugTag, "结果状态JSON解析@" + "status:" + HttpConfig.Success + ";msg:没有回调注册函数");
			}
			return;
		}
		else {
			dataHttpListener.httpCallBack(resultObject, resultData, status);
		}

	}

	/**
	 * 解析网络请求结果为符合回调用的数据
	 *
	 * @param responseData
	 */
	private void parseHttpCallBackSucess(final byte[] responseData) {

		if (httpCallBackFilter(responseData)) {
			httpCallBack(null, responseData, HttpConfig.Success_Filter);
			return;
		}
		else {
			// 对象解析开始
			Object object = HttpConfig.parseResponseData(responseData, cls);
			// 对象解析结束
			// 回调开始
			if (null == object) {
				if (isDebug) {
					Log.i(debugTag, "结果状态JSON解析@" + "status:" + HttpConfig.Success_ParseError + ";msg:对象解析错误");
				}
				httpCallBack(null, responseData, HttpConfig.Success_ParseError);
			}
			else {
				if (isDebug) {
					Log.i(debugTag, "结果状态JSON解析@" + "status:" + HttpConfig.Success + ";msg:对象解析成功");
				}
				if (null != cacheSaveListener) {
					cacheSaveListener.saveResulit(key, object,Base64.encode(responseData));
				}
				httpCallBack(object, responseData, HttpConfig.Success);

			}

		}
	}

	private BinaryHttpResponseHandler getBinaryHttpResponseHandler() {
		BinaryHttpResponseHandler binaryHttpResponseHandler = new BinaryHttpResponseHandler() {
			@Override
			public void onCancel() {
				// TODO Auto-generated method stub
				super.onCancel();
				mRequestHandle = null;
				if (isDebug) {
					Log.i(debugTag, "请求结果@" + "status:" + HttpConfig.Cancle_Http + ";msg:Json请求取消");
				}
				httpCallBack(null, null, HttpConfig.Cancle_Http);
			}

			@Override
			public void onSuccess(int status, Header[] headers, byte[] responseData) {
				mRequestHandle = null;
				if (null == responseData || responseData.length == 0) {
					if (isDebug) {
						Log.i(debugTag, "请求结果@" + "status:" + HttpConfig.Fail + ";msg:失败没有数据返回");
					}
					httpCallBack(null, null, HttpConfig.Fail);
				}
				else {
					if (isDebug) {
						Log.i(debugTag, "请求结果@" + "status:" + HttpConfig.Success + ";msg:返回数据成功");
					}
					parseHttpCallBackSucess(responseData);
				}

			}

			@Override
			public void onFailure(int status, Header[] headers, byte[] errors, Throwable throwable) {
				mRequestHandle = null;
				httpCallBack(null, null, HttpConfig.Fail);

			}
		};
		if (!TextUtils.isEmpty(this.tag)) {
			binaryHttpResponseHandler.setTag(tag);
		}
		return binaryHttpResponseHandler;
	}

	/**
	 * 执行Http请求
	 *
	 * @param cls
	 *            结果解析类型
	 * @param listener
	 *            请求CallBcak监听
	 */
	public void doHttp(Class<?> cls, DataHttpListener listener) {
		this.cls = cls;
		setHttpListener(listener);
		doHttp(null);

	}

	public void doHttp(Context mContext, Class<?> cls, DataHttpListener listener) {
		this.cls = cls;
		setHttpListener(listener);
		doHttp(mContext);

	}

	/**
	 * 执行Http同步请求
	 *
	 * @param cls
	 *            结果解析类型
	 * @param listener
	 *            请求CallBcak监听
	 */
	public ResponseData doHttpSync(Class<?> cls, DataHttpListener listener) {
		this.cls = cls;
		setAjax(false);
		setHttpListener(listener);
		doHttp(null);
		return mResponseData;

	}

	public ResponseData doHttpSync(Context mContext, Class<?> cls, DataHttpListener listener) {
		this.cls = cls;
		setAjax(false);
		setHttpListener(listener);
		doHttp(mContext);
		return mResponseData;

	}

	private void doHttp(Context mContext) {
		if (isDebug) {
			Log.i(debugTag, "请求路径@" + this.Url);

		}
		if (!isTrueHttp()) {
			if (isDebug) {
				Log.i(debugTag, "请求路径不正确");
			}
			httpCallBack(null, null, HttpConfig.Fail_ParamsError);
			return;
		}

		// 设置请求的Key值

		RequestParams mRequestParams = null;
		if (null != mObjectMap && !mObjectMap.isEmpty()) {
			mRequestParams = new RequestParams();
			this.isTrueParams = AsyncHttpConfig.setRequestParamsByObject(mRequestParams, mObjectMap, isDebug, debugTag);
		}

		if (!this.isTrueParams) {
			httpCallBack(null, null, HttpConfig.Fail_ParamsError);
			return;
		}
		getAsyncHttpKey();
		if (TextUtils.isEmpty(this.tag)) {
			this.tag = this.key;
		}
		if (!isEnableHttp()) {
			httpCallBack(null, null, HttpConfig.Fail_ParamsError);
			return;
		}
		//检查并执行请求缓存
		doCacheRead();
		if(!isContinueDoRealHttp())
		{
			return;
		}
		//开始网络请求
		if (null == mContext) {
			if (isPost) {
				if (isAjax) {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientAsync().post(this.Url,
								getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientAsync().post(this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}
				else {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientSync().post(this.Url, getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientSync().post(this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}

			}
			else {
				if (isAjax) {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientAsync().get(this.Url, getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientAsync().get(this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}
				else {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientSync().get(this.Url, getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientSync().get(this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}

			}
		}
		else {
			if (isPost) {
				if (isAjax) {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientAsync().post(mContext, this.Url, new RequestParams(),
								getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientAsync().post(mContext, this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}
				else {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientSync().post(mContext, this.Url, new RequestParams(),
								getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientSync().post(mContext, this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}

			}
			else {
				if (isAjax) {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientAsync().get(mContext, this.Url,
								getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientAsync().get(mContext, this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}
				else {
					if (null == mRequestParams) {
						mRequestHandle = AsyncHttpConfig.getClientSync().get(mContext, this.Url,
								getBinaryHttpResponseHandler());
					}
					else {
						mRequestHandle = AsyncHttpConfig.getClientSync().get(mContext, this.Url, mRequestParams,
								getBinaryHttpResponseHandler());
					}
				}

			}
		}

	}

	/**
	 * 读取缓存的网络请求，如是返回true则不进行网络请求，直接使用缓存数据
	 *
	 * @return
	 */
	private boolean doCacheRead() {

		if (null == cacheGetListener || TextUtils.isEmpty(getAsyncHttpKey())) {
			return false;
		}
		String cacheString = cacheGetListener.getCacheString(this.key);
		if (TextUtils.isEmpty(cacheString)) {
			return false;
		}
		Object object = HttpConfig.parseResponseText( cacheString, cls);
		if (null == object) {
			if (isDebug) {
				Log.i(debugTag, "缓存结果@" + "没有从缓存中读取到结果");
			}
			return false;
		}
		if (isDebug) {
			Log.i(debugTag, "请求结果@" + "从缓存中读取到结果了");
			Log.i(debugTag, "结果JSON缓存@" + cacheString);
		}
		byte[] cacheStringBytes=Base64.decode(cacheString);
		if (isDebug) {
			Log.i(debugTag, "调用httpCallBackCache执行缓存业务逻辑");
		}
		cacheGetListener.httpCallBackCache(object, cacheStringBytes,HttpConfig.Success,cacheGetListener.getCacheTime());
		if(cacheGetListener.isCacheExecuteToHttpCallBack())
		{
			if (isDebug) {
				Log.i(debugTag, "调用httpCallBack执行缓存业务逻辑");
			}
			httpCallBack(object, cacheStringBytes, HttpConfig.Success);
		}
//		else
//		{
//			if (isDebug) {
//				Log.i(debugTag, "调用httpCallBackCache执行缓存业务逻辑");
//			}
//			cacheGetListener.httpCallBackCache(object, cacheStringBytes,HttpConfig.Success,cacheGetListener.getCacheTime());
//		}
		return true;
	}
	//缓存执行后是否继续执行网络请求
	private boolean isContinueDoRealHttp(){
		if(null==cacheGetListener){
			return true;
		}
		else{
			if(cacheGetListener.isCacheOk())
			{
				return cacheGetListener.isDoRealHttp();
			}
			else {
				return true;
			}
		}
	}
}
