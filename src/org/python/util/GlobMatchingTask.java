package org.python.util;

import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;

public abstract class GlobMatchingTask extends FileNameMatchingTask {

    @Override
    protected FileNameMapper createMapper() {
        FileNameMapper mapper = new GlobPatternMapper();
        mapper.setFrom(getFrom());
        mapper.setTo(getTo());
        return mapper;
    }

    protected abstract String getFrom();

    protected abstract String getTo();
}
