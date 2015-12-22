package org.ws.httphelper;
/**
 * <b>描述：</b> 常量<br/>
 * <b>作者：</b> gz <br/>
 * <b>创建时间：</b> 2015-10-12 上午10:16:19 <br/>
 */
public interface WSHttpHelperConstant {
	/**
	 * 连接超时
	 */
	public static String HTTP_CONNECTION_TIMEOUT="http.connection.timeout";
	/**
	 * socket超时
	 */
	public static String HTTP_SOCKET_TIMEOUT="http.socket.timeout";
	/**
	 * 请求编码
	 */
	public static String HTTP_ENCODE_CHARSET="http.encod.charset";
	
	public static String POOL_QUEUEC_APACITY="pool.QueueCapacity";
	public static String POOL_CORE_POOL_SIZE="pool.CorePoolSize";
	public static String POOL_MAX_POOL_SIZE="pool.MaxPoolSize";
	public static String POOL_KEEP_ALIVE_SECONDS="pool.KeepAliveSeconds";

    /**
     * 默认处理器
     */
    public static final String DEFAULT_HANDLER_PRE_INIT="default.handler.pre.init";
    public static final String DEFAULT_HANDLER_PRE_PARAMETER="default.handler.pre.parameter";
    public static final String DEFAULT_HANDLER_PRE_URL="default.handler.pre.url";
    public static final String DEFAULT_HANDLER_PRE_VALIDATION="default.handler.pre.validation";

    public static final String DEFAULT_HANDLER_PRO_PARSE="default.handler.pro.parse";

	/**
	 * 前处理级别：第一步初始化
	 */
	public static final int PRE_HANDLER_INIT=0;

	/**
	 * 前处理级别：第二步验证参数
	 */
	public static final int PRE_HANDLER_VALIDATION=1;

	/**
	 * 前处理级别：第三步生成URL和参数
	 */
	public static final int PRE_HANDLER_BUILD_PARAM=2;
	/**
	 * 前处理级别：自定义
	 */
	public static  final  int PRE_HANDLER_USER=3;
	/**
	 * 后处理级别：第一步解析结果
	 */
	public static final int PRO_HANDLER_PARSE=0;
	/**
	 * 后处理级别：自定义
	 */
	public static final int PRO_HANDLER_USER=1;
}
