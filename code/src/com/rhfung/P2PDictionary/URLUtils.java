package com.rhfung.P2PDictionary;

import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by richard on 1/18/16.
 */
class URLUtils {
    /**
     * Removes extra backslash.
     * @param readableURL
     * @return
     */
    static String URLEncode(String readableURL)
    {
        if (readableURL == null) {
            return readableURL;
        }
        if (readableURL.equals("/")) {
            return readableURL;
        }
        try
        {
            String[] parts = readableURL.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = URLEncoder.encode(parts[i], "utf-8");
            }
            return  StringUtils.join(parts, "/");
        }
        catch(Exception ex)
        {
            return readableURL;
        }
    }

    /**
     * Removes extra backslash.
     * @param encodedURL
     * @return
     */
    static String URLDecode(String encodedURL)
    {
        if (encodedURL == null) {
            return encodedURL;
        }
        if (encodedURL.equals("/")) {
            return encodedURL;
        }
        try
        {
            String[] parts = encodedURL.split("/");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = URLDecoder.decode(parts[i], "utf-8");
            }
            return StringUtils.join(parts, "/");
        }
        catch(Exception ex)
        {
            return encodedURL;
        }
    }


    /*
        * Splits a string into up to three parts:
        * first
        * first last
        * first middle middle last
        */
    static String[] splitFrontEnd3(String input)
    {
        int firstSpace = input.indexOf(" ");
        int lastSpace = input.indexOf(" ", firstSpace + 1);
        if (0 < firstSpace  && firstSpace < lastSpace)
        {
            // three-part string formed
            return new String[] { input.substring(0, firstSpace),
                    input.substring(firstSpace + 1, lastSpace),
                    input.substring(lastSpace + 1) };
        }
        else
        {
            if (firstSpace > 0)
                return new String[] { input.substring(0, firstSpace), input.substring(firstSpace + 1) };
            else
                return new String[] { input };
        }
    }
}
