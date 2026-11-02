package com.plummy.vio.datapack;

import java.util.List;

public record DatapackWriteResult(boolean needsRestart, List<String> warnings) {
}
