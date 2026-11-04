package com.plummy.vio.core.agent;

import com.plummy.vio.core.user.User;

public interface Agent {

    SubmitResult submit(User user, String request);
}
