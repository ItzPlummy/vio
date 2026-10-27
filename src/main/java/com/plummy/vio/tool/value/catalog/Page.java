package com.plummy.vio.tool.value.catalog;

import com.plummy.vio.tool.value.argument.Argument;
import com.plummy.vio.tool.value.result.ResultValue;
import com.plummy.vio.tool.value.result.ResultValueVisitor;

import java.util.List;

public class Page implements Argument, ResultValue {

    private final int limit;
    private final int offset;
    private final Integer total;

    public Page(int limit, int offset) {
        this(limit, offset, null);
    }

    private Page(int limit, int offset, Integer total) {
        this.limit = limit;
        this.offset = offset;
        this.total = total;
    }

    @Override
    public <T> T accept(ResultValueVisitor<T> visitor) {
        return visitor.visitPage(this);
    }

    public Page withTotal(int total) {
        return new Page(limit, offset, total);
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public Integer getTotal() {
        return total;
    }

    public Integer getNextOffset() {
        return offset + limit < total ? offset + limit : null;
    }

    public <T> List<T> apply(List<T> items) {
        if (getOffset() >= items.size()) {
            return List.of();
        }

        return items.subList(getOffset(), Math.min(items.size(), getLimit() + getOffset()));
    }
}
