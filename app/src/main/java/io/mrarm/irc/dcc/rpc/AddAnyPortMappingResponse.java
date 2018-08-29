package io.mrarm.irc.dcc.rpc;

import org.w3c.dom.Element;

import io.mrarm.irc.dcc.XMLParseHelper;

public class AddAnyPortMappingResponse {

    private int mNewReservedPort;

    public AddAnyPortMappingResponse(Element response) {
        mNewReservedPort = Integer.parseInt(XMLParseHelper.getChildElementValue(response,
                "NewReservedPort", ""));
    }

    public int getNewReservedPort() {
        return mNewReservedPort;
    }
}
