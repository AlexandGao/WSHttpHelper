package org.ws.httphelper.request;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.message.BasicNameValuePair;
import org.ws.httphelper.WSHttpHelperConfig;
import org.ws.httphelper.WSHttpHelperConstant;
import org.ws.httphelper.annotation.Header;
import org.ws.httphelper.annotation.Parameter;
import org.ws.httphelper.annotation.WSRequest;
import org.ws.httphelper.common.MapKeyComparator;
import org.ws.httphelper.exception.WSException;
import org.ws.httphelper.model.ErrorMessage;
import org.ws.httphelper.model.ResponseResult;
import org.ws.httphelper.request.handler.RequestPreHandler;
import org.ws.httphelper.request.handler.ResponseProHandler;
import org.ws.httphelper.request.handler.impl.pre.DefaultInitHandlerImpl;
import org.ws.httphelper.request.handler.impl.pre.DefaultParameterBuliderHandlerImpl;
import org.ws.httphelper.request.handler.impl.pre.DefaultURLBuilderHandlerImpl;
import org.ws.httphelper.request.handler.impl.pre.DefaultValidationHandlerImpl;
import org.ws.httphelper.request.handler.impl.pro.DefaultResultParseHandlerImpl;
import org.ws.httphelper.request.handler.HandlerFactory;
import org.ws.httphelper.http.HttpTaskExecutor;
import org.ws.httphelper.model.ParameterDefine;
import org.ws.httphelper.model.WSRequestContext;

import java.util.*;

/**
 * 请求的抽象类<br/>
 * 实现请求执行过程，执行过程如下：<br/>
 * 1.根据<b>@WSRequest</b>注解描述生成请求上下文。若没有<b>@WSRequest</b>注解则抛出异常。<br/>
 * 2.调用init(context)方法执行初始化，主要是向contex中添加数据，或者添加自定义处理器。<br/>
 * 3.添加默认处理器。请求前处理顺序：默认值初始化，验证参数，生成请求参数，生成URL。请求后处理：解析结果。<br/>
 * 3.1首选根据配置文件获取指定的默认处理器，若配置文件中没有指定，则使用org.ws.httphelper.request.handler.impl包中的默认处理器。<br/>
 * 4.执行请求前处理,按照顺序依次执行。<br/>
 * 4.1默认初始化处理：若注解描述中存在默认值，并且参数没有输入值，则为参数设置该默认值、<br/>
 * 4.2验证参数处理：根据注解描述，验证必须值，验证输入参数类型，根据正则验证。<br/>
 * 4.3生成请求参数处理：根据配置的参数或者动态添加的参数生成请求参数。自动识别普通参数，数组参数，文件参数。<br/>
 * 4.4生成URL处理：根据输入key的值自动匹配替换URL中{key}的值。<br/>
 * 5.执行请求。<br/>
 * 6.执行请求后处理，按照顺序依次执行。<br/>
 * 6.1解析结果处理：只解析JSON为指定的对象。<br/>
 * 7.清理缓存。不清理Cookie。要清除Cookie通过context.clearCookie()。<br/>
 * Created by gz on 15/12/4.
 */
public abstract class WSHttpAbstractRequest implements WSHttpRequest{
    protected static Log log = LogFactory.getLog(WSHttpAbstractRequest.class);

    private WSRequestContext context = new WSRequestContext();
    protected Map<Integer,List<RequestPreHandler>> requestPreHandlerListMap = new TreeMap<Integer, List<RequestPreHandler>>(new MapKeyComparator());
    protected Map<Integer,List<ResponseProHandler>> responseProHandlerListMap = new TreeMap<Integer,List<ResponseProHandler>>(new MapKeyComparator());

    public abstract void init(WSRequestContext context)throws WSException;

    public ResponseResult execute()throws WSException{
        builderContext();
        ResponseResult result=null;
        init(context);
        // 请求处理
        defaultHandlerInit();
        // 执行前处理
        Set<Integer> preKeySet = requestPreHandlerListMap.keySet();
        // 从小到大以此执行
        for(Integer key:preKeySet){
            List<RequestPreHandler> list = requestPreHandlerListMap.get(key);
            if(list!=null){
                for(RequestPreHandler handler:list){
                    if(!handler.handler(context)){
                        // 出现错误：获取错误消息并返回
                        result = new ResponseResult();
                        result.setStatus(999);
                        List<ErrorMessage> errorMessageList=context.getErrorMessageList();
                        StringBuffer error = new StringBuffer();
                        for(ErrorMessage message:errorMessageList){
                            error.append(message);
                        }
                        result.setBody(error.toString());
                        return result;
                    }
                }
            }
        }
        // 执行请求
        String uuid=HttpTaskExecutor.getInstance().execute(context);
        // 获取执行结果
        result=HttpTaskExecutor.getInstance().getResult(uuid);
        // 执行后处理
        Set<Integer> proKeySet = responseProHandlerListMap.keySet();
        // 从小到大以此执行
        for(Integer key:proKeySet){
            List<ResponseProHandler> list = responseProHandlerListMap.get(key);
            if(list!=null){
                for(ResponseProHandler handler:list){
                    result=handler.handler(context,result);
                }
            }
        }
        // 清楚缓存
        clear();
        return result;
    }

    private void clear(){
        this.context.clear();
        requestPreHandlerListMap.clear();
        responseProHandlerListMap.clear();
    }

    public void addRequestPreHandler(RequestPreHandler handler){
        if(handler==null){
            return;
        }
        int level = handler.level();
        if(requestPreHandlerListMap.containsKey(level)){
            List<RequestPreHandler> list = requestPreHandlerListMap.get(level);
            list.add(handler);
        }
        else{
            List<RequestPreHandler> list = new ArrayList<RequestPreHandler>();
            list.add(handler);
            requestPreHandlerListMap.put(level,list);
        }
    }

    public void addResponseProHandler(ResponseProHandler handler){
        if(handler==null){
            return;
        }
        int level = handler.level();
        if(responseProHandlerListMap.containsKey(level)){
            List<ResponseProHandler> list = responseProHandlerListMap.get(level);
            list.add(handler);
        }
        else{
            List<ResponseProHandler> list = new ArrayList<ResponseProHandler>();
            list.add(handler);
            responseProHandlerListMap.put(level,list);
        }
    }

    /**
     * 添加请求头部
     * @param name
     * @param value
     */
    public void addHeader(String name,String value){
        context.addHeader(name, value);
    }

    public void addCookie(String name,String value){context.addCookie(name,value);}

    /**
     * 添加请求参数
     * @param name
     * @param value
     */
    public void addParameter(String name,Object value){
        context.addParameterDefineList(new ParameterDefine(name))
                .addInputData(name,value);
    }

    private void builderContext()throws WSException{
        WSRequest ann = this.getClass().getAnnotation(WSRequest.class);
        if(ann!=null){
            context.setName(ann.name());
            if(StringUtils.isEmpty(context.getUrl())){
                context.setUrl(ann.url());
            }
            if(StringUtils.isEmpty(context.getDescription())){
                context.setDescription(ann.description());
            }
            if(context.getResponseType()==null){
                context.setResponseType(ann.responseType());
            }
            if(context.getMethod()==null){
                context.setMethod(ann.method());
            }

            String charset=ann.charset();
            if(StringUtils.isEmpty(charset)){
                charset= WSHttpHelperConfig.getCharset();
            }
            if(StringUtils.isEmpty(context.getCharset())){
                context.setCharset(charset);
            }
            if(context.getResultClass()==null){
                context.setResultClass(ann.resultClass());
            }

            if(ann.headers()!=null){
                for(Header header:ann.headers()){
                    context.addHeader(header.name(),header.value());
                }
            }
            if(ann.parameters()!=null){
                for(Parameter parameter:ann.parameters()){
                    context.addParameterDefineList(builderParameterDefine(parameter));
                }
            }
        }
        else{
            throw new WSException("不是有效的请求，无法生成上下文！");
        }
    }

    /**
     * 获取参数
     * @param parameter
     * @return
     * @throws WSException
     */
    private ParameterDefine builderParameterDefine(Parameter parameter)throws WSException{
        ParameterDefine parameterDefine = new ParameterDefine();
        parameterDefine.setName(parameter.name());
        parameterDefine.setDescription(parameter.description());
        parameterDefine.setDefaultValue(parameter.defaultValue());
        parameterDefine.setExample(parameter.example());
        parameterDefine.setRequired(parameter.required());
        parameterDefine.setType(parameter.type());
        parameterDefine.setValidateRegex(parameter.validateRegex());
        return parameterDefine;
    }

    /**
     * 初始化默认处理器
     * @throws WSException
     */
    private void defaultHandlerInit() throws WSException {
        // 前处理
        String[] proHandlerList={getDefaultHandlerName(WSHttpHelperConstant.DEFAULT_HANDLER_PRE_INIT, DefaultInitHandlerImpl.class),
                getDefaultHandlerName(WSHttpHelperConstant.DEFAULT_HANDLER_PRE_PARAMETER, DefaultParameterBuliderHandlerImpl.class),
                getDefaultHandlerName(WSHttpHelperConstant.DEFAULT_HANDLER_PRE_URL, DefaultURLBuilderHandlerImpl.class),
                getDefaultHandlerName(WSHttpHelperConstant.DEFAULT_HANDLER_PRE_VALIDATION, DefaultValidationHandlerImpl.class)};
        for(String className:proHandlerList){
            addRequestPreHandler(HandlerFactory.finadHandler(RequestPreHandler.class,className));
        }
        // 后处理
        addResponseProHandler(HandlerFactory.finadHandler(ResponseProHandler.class,
                getDefaultHandlerName(WSHttpHelperConstant.DEFAULT_HANDLER_PRO_PARSE,DefaultResultParseHandlerImpl.class)));
    }

    /**
     * 获取配置处理类，若没有配置使用默认的
     * @param configKey
     * @param defaultHandler
     * @return
     * @throws WSException
     */
    private String getDefaultHandlerName(String configKey,Class defaultHandler) throws WSException {
        String className=WSHttpHelperConfig.getValue(configKey);
        if(className==null){
            className=defaultHandler.getName();
        }
        return className;
    }

    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("<h1>").append(context.getName()).append("</h1><br/>");
        help.append("<h4>请求路径：").append(context.getUrl()).append("</h4><br/>");
        help.append("");
        return help.toString();
    }

    public WSRequestContext getContext() {
        return context;
    }
}
