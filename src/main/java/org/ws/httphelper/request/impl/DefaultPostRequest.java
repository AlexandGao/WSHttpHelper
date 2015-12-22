package org.ws.httphelper.request.impl;

import org.ws.httphelper.annotation.WSRequest;
import org.ws.httphelper.exception.WSException;
import org.ws.httphelper.model.WSRequestContext;
import org.ws.httphelper.request.WSHttpAbstractRequest;

@WSRequest(
        name="默认POST请求",url="{url}",method= WSRequest.MethodType.POST
)
public class DefaultPostRequest extends WSHttpAbstractRequest {
    @Override
    public void init(WSRequestContext context) throws WSException {

    }
}
