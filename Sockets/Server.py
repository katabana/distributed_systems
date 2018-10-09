import socket
import sys
import _thread
import signal
import struct
from concurrent.futures import ThreadPoolExecutor

CONST_MSG_SIZE = 4096
server_address = ('localhost', 12345)
mcast_address = ('225.0.0.250', 8123)
active_connections = []
udp_addresses = {}
last_guest = 0
thread_lock = _thread.allocate_lock()

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
mcast_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
mcast_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)

group = socket.inet_aton(mcast_address[0])
mreq = struct.pack('4sl', group, socket.INADDR_ANY)
mcast_socket.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, mreq)

server_socket.bind(server_address)
udp_socket.bind(server_address)
mcast_socket.bind(mcast_address)

server_socket.listen()

print('Starting up on %s port %s' % server_address)


def signal_handler(signal, frame):
    print('\rDisconnected.')
    sys.exit(0)


def socket_error_handler(exception, message):
    print(exception)
    print(message)
    sys.exit(1)


def tcp_send_further(message, nickname):
    data = make_message(nickname + ': ' + message)
    for c in active_connections:
        if c[2] != nickname:
            try:
                c[0].sendall(data)
                print('Sent TCP message to', c[2])
            except socket.error as e:
                socket_error_handler(e, 'Failed to send TCP message')


def udp_send_further(message, address):
    data = make_message(udp_addresses[address] + ': ' + message[2:])
    for c in udp_addresses:
        if c != address:
            try:
                udp_socket.sendto(data, c)
                print('Sent UDP message to', c)
            except socket.error as e:
                socket_error_handler(e, 'Failed to send UDP message')


def make_message(message):
    return bytes(message, 'utf-8')


def maintain_connection(info):
    (connection, client_address, nickname) = info
    try:
        print('Connection from', client_address)

        # Receive the data in small chunks and retransmit it
        while True:
            data = connection.recv(CONST_MSG_SIZE)
            content = str(data.decode('utf-8'))
            if data:
                if content.startswith("U "):
                    # add to udp_connections
                    port = int(content[2:])
                    udp_addresses[('127.0.0.1', port)] = nickname
                    print('Added UDP address of', nickname)
                else:
                    thread_lock.acquire()
                    print('Received TCP message from', nickname)
                    tcp_send_further(content, nickname)
                    thread_lock.release()
            else:
                print('Disconnected:', client_address)
                break
    finally:
        active_connections.remove(info)
        print('Removed TCP connection:', info[1:])
        return  # also ends the thread


def listen_udp():
    try:
        while True:
            buff, address = udp_socket.recvfrom(CONST_MSG_SIZE)
            thread_lock.acquire()
            message = str(buff, 'utf-8')
            print('Received UDP message from', address)
            if message != 'hello':
                udp_send_further(message, address)
            thread_lock.release()
    except socket.error as e:
        print(e)


def listen_mcast():
    try:
        while True:
            data, sender = mcast_socket.recvfrom(CONST_MSG_SIZE)
            message = str(data, 'utf-8')
            print("Received UDP multicast \'" + message + '\'')
    except socket.error as e:
        print(e)


def new_nickname():
    global last_guest
    last_guest += 1
    return "guest" + str(last_guest)


def main():
    _thread.start_new_thread(listen_udp, ())
    _thread.start_new_thread(listen_mcast, ())
    signal.signal(signal.SIGINT, signal_handler)
    pool = ThreadPoolExecutor(10)

    while True:
        connection, client_address = server_socket.accept()
        nickname = new_nickname()
        connection_data = (connection, client_address, nickname)
        active_connections.append(connection_data)
        print('Added new connection:', connection_data[1:])

        try:
            pool.submit(maintain_connection, connection_data)
        except _thread.error as e:
            print("Error: unable to start the thread")
            print(e)

if __name__ == "__main__":
    main()
