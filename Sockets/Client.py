import socket
import sys
import _thread
import signal

CONST_MSG_SIZE = 4096
client = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
udp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
mcast_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
mcast_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
prompt = 'You: '

server_address = ('localhost', 12345)
mcast_address = ('225.0.0.250', 8123)


def signal_handler(signal, frame):
    print("\rDisconnected.")
    sys.exit(0)


def socket_error_handler(exception, message):
    print(exception)
    print(message)
    sys.exit(1)


def print_message(message):
    print("\r", message + "\n\r" + prompt, end="")


def make_message(message):
    return bytes(message, 'utf-8')


def receive_tcp_messages():
    while True:
        try:
            data = client.recv(CONST_MSG_SIZE)
            if data:
                message_received = data.decode('utf-8')
                print_message(message_received)
            else:
                break
        except socket.error as e:
            socket_error_handler(e, "TCP receive failed")


def receive_udp_messages():
    try:
        while True:
            buff, address = udp_socket.recvfrom(1024)
            message = str(buff, 'utf-8')
            print_message(message)
    except socket.error as e:
        socket_error_handler(e, "UDP receive failed")


def receive_mcast_messages():
    try:
        while True:
            data, sender = mcast_socket.recvfrom(1024)
            message = str(data, 'utf-8')
            print_message(message)
    except socket.error as e:
        print(e)


def send_udp(message):
    data = make_message(message)
    udp_socket.sendto(data, server_address)


def send_multicast(message):
    data = make_message(message[2:])
    try:
        mcast_socket.sendto(data, mcast_address)
    except socket.error as e:
        print(e)


def main():
    try:
        mcast_socket.bind(mcast_address)
        client.connect(server_address)
        signal.signal(signal.SIGINT, signal_handler)

        # send port number via TCP
        udp_socket.sendto(make_message('hello'), server_address)
        udp_port = udp_socket.getsockname()[1]
        message = "U " + str(udp_port)
        client.sendall(make_message(message))

        try:
            _thread.start_new_thread(receive_tcp_messages, ())
            _thread.start_new_thread(receive_udp_messages, ())
            _thread.start_new_thread(receive_mcast_messages, ())
        except _thread.error:
            print("Error: unable to start the listening thread")

        while True:
            try:
                message = input("\r" + prompt)
                if message.startswith("U "):
                    send_udp(message)
                elif message.startswith("M "):
                    send_multicast(message)
                else:
                    client.sendall(make_message(message))
            except socket.error as e:
                socket_error_handler(e, "Server is unreachable")

    except socket.error as e:
        socket_error_handler(e, "Unable to connect")
    finally:
        print('Closing client')

if __name__ == "__main__":
    main()
