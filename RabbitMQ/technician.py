import pika
import sys
import signal

#TODO: copy fanout from doctor.py (hospital1)

specializations = ['knee', 'elbow', 'hip']

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
channel = connection.channel()

def signal_handler(signal, frame):
    print("\rDisconnected.")
    channel.stop_consuming()
    sys.exit(0)

def create_message(body):
    doc_id, examination_type, patient_name = body.split(';')
    message = patient_name + ';' + examination_type + ';done'
    return doc_id, message


def send_message(ch, body):
    doc_id, message = create_message(body)
    ch.basic_publish(exchange='hospital2',
                     routing_key='hospital2.' + doc_id,
                     body=message)


def callback(ch, method, properties, body):
    print(" [x] %r:%r" % (method.routing_key, body))
    send_message(ch, body)
    ch.basic_ack(delivery_tag = method.delivery_tag)


def admin_callback(ch, method, properties, body):
    print('Admin said: %r' % body)
    ch.basic_ack(delivery_tag = method.delivery_tag)


signal.signal(signal.SIGINT, signal_handler)

channel.exchange_declare(exchange='hospital2',
                         exchange_type='topic')

channel.exchange_declare(exchange='hospital_ad',
                         exchange_type='topic')

spec_1 = ""
while spec_1 not in specializations:
    spec_1 = raw_input("First specialization: ")

spec_2 = ""
while spec_2 not in specializations:
    spec_2 = raw_input("Second specialization ")


channel.queue_declare(queue=spec_1)
channel.queue_declare(queue=spec_2)
queue_name = channel.queue_declare(exclusive=True).method.queue

key_1 = 'hospital2.' + spec_1
key_2 = 'hospital2.' + spec_2

channel.queue_bind(exchange='hospital2',
                       queue=spec_1,
                       routing_key=key_1)

channel.queue_bind(exchange='hospital2',
                       queue=spec_2,
                       routing_key=key_2)

channel.queue_bind(exchange='hospital_ad',
                       queue=queue_name,
                       routing_key='hospital_ad.*')


channel.basic_consume(callback,
                      queue=spec_1)

channel.basic_consume(callback,
                      queue=spec_2)

channel.basic_consume(admin_callback,
                      queue=queue_name)

channel.start_consuming()
