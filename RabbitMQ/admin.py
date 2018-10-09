import pika
import sys
import signal
import threading


def signal_handler(signal, frame):
    print("\rDisconnected.")
    global connection
    connection.close()
    sys.exit(0)


def callback(ch, method, properties, body):
    print(" [log] %r:%r" % (method.routing_key, body))
    ch.basic_ack(delivery_tag=method.delivery_tag)


def in_thread():
    channel.start_consuming()


signal.signal(signal.SIGINT, signal_handler)
connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='hospital2',
                         exchange_type='topic')

channel.exchange_declare(exchange='hospital_ad',
                         exchange_type='topic')

queue_name = channel.queue_declare(exclusive=True).method.queue

key = 'hospital2.*'
channel.queue_bind(exchange='hospital2',
                       queue=queue_name,
                       routing_key=key)


channel.basic_consume(callback,
                      queue=queue_name)

t = threading.Thread(target=in_thread)
t.start()

while True:
    content = raw_input("Message: ")
    message = content

    channel.basic_publish(exchange='hospital_ad',
                          routing_key='hospital_ad.admin',
                          body=message)
