import os
import matplotlib.pyplot as plt
import math

def extract_query_times(file_name, N_skip):
    data = []
    file = open(file_name)
    for ii in range(N_skip):
        file.readline()
    for line in file.readlines():
        line = line.split(',')
        data += [(int(line[0]), (int(line[1]) + int(line[2]))/10**6)]

    file.close()

    return(data)


def smoothing(times, grouping):
    data = {}
    for time in times:
        x = math.ceil(time[0] / grouping) * grouping
        if x not in data:
            data[x] = []
        data[x] += [time[1]]

    results = {}
    for x in data:
        if len(data[x]) > 5:
            data[x] = sorted(data[x])
        results[x] = sum(data[x]) / len(data[x])

    xs = sorted(results.keys())
    ys = [results[x] for x in xs]
    return (xs, ys)


grouping = 200

time_swissse    = extract_query_times('.\\outputs\\insert_400000.txt', 7)
x_swissse, y_swissse = smoothing(time_swissse, grouping)

# plot insert time
plt.plot(x_swissse, y_swissse)
plt.xlim([-500,20500])

plt.yscale('log')

plt.title("Insertion Time", fontsize=16)
plt.xlabel('Min frequency of the keyword in the document', fontsize=14)
plt.ylabel('Insertion Time (ms)',fontsize=14)
#plt.legend(('Plaintext', 'dynamic SWiSSSE'), fontsize=14)

plt.xticks(fontsize=12)
plt.yticks(fontsize=14)

plt.show()
