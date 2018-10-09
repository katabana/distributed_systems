import pika
import sys
import signal
import threading


def signal_handler(signal, frame):
    print("\rDisconnected.")
    connection.close()
    sys.exit(0)


def callback(ch, method, properties, body):
    if method.routing_key.startswith('hospital_ad'):
        print('Admin said: %r' % body)
    else:
        print(" [x] %r:%r" % (method.routing_key, body))
    ch.basic_ack(delivery_tag=method.delivery_tag)


def in_thread():
    channel.start_consuming()


specializations = ['knee', 'elbow', 'hip']


signal.signal(signal.SIGINT, signal_handler)

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

channel.exchange_declare(exchange='hospital2',
                         exchange_type='topic')

channel.exchange_declare(exchange='hospital_ad',
                         exchange_type='topic')

queue_name = channel.queue_declare(exclusive=True).method.queue

pid = raw_input("Id: ")
while pid in specializations or pid == "":
    pid = raw_input("Cannot use reserved: knee, elbow or hip. Id: ")

key = 'hospital2.' + pid

channel.queue_bind(exchange='hospital2',
                       queue=queue_name,
                       routing_key=key)

channel.queue_bind(exchange='hospital_ad',
                       queue=queue_name,
                       routing_key='hospital_ad.*')


channel.basic_qos(prefetch_count=1)
channel.basic_consume(callback,
                      queue=queue_name)


t = threading.Thread(target=in_thread)
t.start()


while True:
    examination_type = raw_input("Type: ")
    while examination_type not in specializations:
        examination_type = raw_input("Give proper type: ")
    patient_name = raw_input("Name: ")
    order = pid + ';' + examination_type + ';' + patient_name

    channel.basic_publish(exchange='hospital2',
                          routing_key=('hospital2.' + examination_type),
                          body=order)
