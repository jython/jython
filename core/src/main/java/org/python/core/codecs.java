package org.python.core;

import org.python.base.MissingFeature;

/** Stop-gap definition to satisfy references in the project. */
class codecs {

    static int insertReplacementAndGetResume(StringBuilder v, String errors, String string,
            String str, int loopStart, int i, String string2) {
        // TODO Auto-generated method stub
        return 0;
    }

    static String PyUnicode_EncodeASCII(String s, int length, Object object) {
        // TODO Auto-generated method stub
        return null;
    }

    static Object encoding_error(String errors, String encoding, String toEncode, int start,
            int end, String reason) {
        throw new MissingFeature("codecs.java");
    }

}
