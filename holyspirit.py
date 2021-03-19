import concurrent.futures, keyboard, time

n = time.time_ns()
print(n)
def godclock():
    global n
    while True:
        n += 1
        # time.sleep(1)


if __name__ == '__main__':
    with concurrent.futures.ThreadPoolExecutor() as executor:
        executor.submit(godclock)
    while not keyboard.is_pressed('space'):
        continue
    else:
        print(n)
    # print(n)
    # while not keyboard.is_pressed('space'):
    #     godclock += 1
    # else:
    #     print(godclock)
