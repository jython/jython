
import socket
import thread
import time


def server():
    HOST = ''                 # Symbolic name meaning the local host
    PORT = 50007              # Arbitrary non-privileged port
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind((HOST, PORT))
    #print "server listen", s
    s.listen(1)
    #print "server accept", s
    conn, addr = s.accept()
    #print 'Connected by', addr
    while 1:
        data = conn.recv(1024)
        if not data: break
        conn.send(data)
    conn.close()


def client():
    HOST = '127.0.0.1'        # The remote host
    PORT = 50007              # The same port as used by the server
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    #print "client connect", s
    s.connect((HOST, PORT))
    #print "client send"
    s.send('Hello, world')
    f = s.makefile()
    s.close()
    data = f.read(12)
    f.close()
    #print 'Received', `data`



thread.start_new_thread(server, ())
time.sleep(5)
client()

