package com.rocket.pan.server.modules.share.converter;

import com.rocket.pan.server.modules.share.context.CreateShareUrlContext;
import com.rocket.pan.server.modules.share.po.CreateShareUrlPO;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-11-26T01:27:46+0800",
    comments = "version: 1.5.2.Final, compiler: javac, environment: Java 11.0.18 (Oracle Corporation)"
)
@Component
public class ShareConverterImpl implements ShareConverter {

    @Override
    public CreateShareUrlContext createShareUrlPO2CreateShareUrlContext(CreateShareUrlPO createShareUrlPO) {
        if ( createShareUrlPO == null ) {
            return null;
        }

        CreateShareUrlContext createShareUrlContext = new CreateShareUrlContext();

        createShareUrlContext.setShareName( createShareUrlPO.getShareName() );
        createShareUrlContext.setShareType( createShareUrlPO.getShareType() );
        createShareUrlContext.setShareDayType( createShareUrlPO.getShareDayType() );

        createShareUrlContext.setUserId( com.rocket.pan.server.common.utils.UserIdUtil.get() );

        return createShareUrlContext;
    }
}
