package com.plummy.vio.core.tool.argument;

import java.util.List;

public record ArgumentDefinition(String type, String description, List<String> options) {
}
