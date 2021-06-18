import psycopg2
try:
    conn = psycopg2.connect(
        host="localhost", database="postgres", user="role1", password="role1")
    # create a cursor
    cur = conn.cursor()

    # execute a statement
    print('PostgreSQL database version:')
    cur.execute('SELECT version()')

    # display the PostgreSQL database server version
    db_version = cur.fetchone()
    print(db_version)

    # OK, lets start improvising ;)
    cur.execute('SELECT * FROM ticks')
    db_ticks_output = cur.fetchone()
    print(db_ticks_output)

    conn.close()

except psycopg2.OperationalError as ex:
    print("Connection failed: {0}".format(ex))
