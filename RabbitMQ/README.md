## RabbitMQ

Example application using RabbitMQ. 

There are three types of users:
- Doctor (ordering examinations, receiving results)
- Technician (doing examinations, sending results)
- Administrator (logging events, can send message to everyone)

There are three types of examinations: hip, knee and elbow.

Doctor sends order of examination, providing the type of examination and patient's name to any technician that can do the examination. Results are returned asynchronously

Each technician can do two types of examination and gets only orders of those types, sending response to the doctor that has ordered it. Only one technician does received order. 

Administrator loggs all events (receiving copies of messages - orders and responses). He can send message to all users.

* * *

Only for Python 2.

**Start a new doctor:**

```linux
python2 doctor.py
```
When a new doctor is started you will be asked for his id.
Then you can order new examination by providing the type of examination and next the patient's name.

**Start a new technician:**

```linux
python2 technician.py
```
When a new technician is started you will be asked for his two specialisations.

**Start the admin:**
```linux
python2 admin.py
```

