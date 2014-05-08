# dispatches to _socket for actual implementation

from _socket import (
    POLLIN,
    POLLOUT,
    POLLPRI,
    POLLERR,
    POLLHUP,
    POLLNVAL,
    error,
    #poll,
    select)
