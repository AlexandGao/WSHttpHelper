package org.ws.httphelper.http;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.ws.httphelper.WSHttpHelperConfig;
import org.ws.httphelper.WSHttpHelperConstant;
import org.ws.httphelper.exception.WSException;
import org.ws.httphelper.model.WSRequestContext;
import org.ws.httphelper.model.ResponseResult;

public class HttpTaskExecutor {
	private ThreadPoolExecutor threadPool = null;
	private Map<String,FutureTask<Object>> taskMap = new HashMap<String,FutureTask<Object>>();
    private static HttpTaskExecutor _instance = new HttpTaskExecutor();

    public static HttpTaskExecutor getInstance(){
        return _instance;
    }
    
    public HttpTaskExecutor(){
    	threadPool = new ThreadPoolExecutor(WSHttpHelperConfig.getInt(WSHttpHelperConstant.POOL_CORE_POOL_SIZE),
    			WSHttpHelperConfig.getInt(WSHttpHelperConstant.POOL_MAX_POOL_SIZE),
    			WSHttpHelperConfig.getInt(WSHttpHelperConstant.POOL_KEEP_ALIVE_SECONDS),
    			TimeUnit.SECONDS,
    			new ArrayBlockingQueue<Runnable>(WSHttpHelperConfig.getInt(WSHttpHelperConstant.POOL_QUEUEC_APACITY)),
    			new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public String execute(WSRequestContext context)throws WSException{
        FutureTask<Object> task = new FutureTask<Object>(new WSHttpClient(context));
        threadPool.submit(task);
        String GUID = UUID.randomUUID().toString();
        taskMap.put(GUID, task);
        return GUID;
    }
    
    public ResponseResult getResult(String GUID)throws WSException{
        FutureTask<Object> task = taskMap.get(GUID);
        try{
            return (ResponseResult)task.get();
        }
        catch (Exception e) {
            ResponseResult result = new ResponseResult();
            result.setStatus(500);
            result.setWasteTime(-1);
            result.setBody(e.getMessage());
            return result;
        }
        finally{
            taskMap.remove(GUID);
        }
    }
}
