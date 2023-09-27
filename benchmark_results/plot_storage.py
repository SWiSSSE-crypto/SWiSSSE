import os
import matplotlib.pyplot as plt

def extract_storage_plaintext(file_name, N_skip):
    data = {}
    file = open(file_name)

    for ii in range(N_skip):
        file.readline()
        
    data['index'] = int(file.readline().split(':')[1]) * 2 / 1024 / 1024 
    data['document'] = int(file.readline().split(':')[1]) / 1024 / 1024
    file.close()
    return(data)


def extract_storage_plaintext_swissse(file_name, N_skip):
    data = {}
    file = open(file_name)

    for ii in range(N_skip):
        file.readline()
        
    data['index'] = int(file.readline().split(':')[1]) * 50 / 1024 / 1024 
    data['document'] = int(file.readline().split(':')[1]) / 1024 / 1024
    file.close()
    return(data)



data_plaintext = {}
for N_emails in [10000, 50000, 100000, 200000, 400000]:
    filename = f'./outputs/plaintext_{N_emails}.txt'
    data_plaintext[N_emails] = extract_storage_plaintext(filename, 1)

data_swissse = {}
for N_emails in [10000, 50000, 100000, 200000, 400000]:
    filename = f'./outputs/search_{N_emails}.txt'
    data_swissse[N_emails] = extract_storage_plaintext_swissse(filename, 1)



# plot KDP
xs = sorted(data_plaintext.keys())
index_plaintext   = [data_plaintext[x]['index'] for x in xs]
plt.scatter(xs, index_plaintext, marker = 'o', c = 'blue')

document_plaintext   = [data_plaintext[x]['document'] for x in xs]
plt.scatter(xs, document_plaintext, marker = 'x', c = 'blue')


xs = sorted(data_swissse.keys())
index_swissse     = [data_swissse[x]['index'] for x in xs]
plt.scatter(xs, index_swissse, marker = 'o', c = 'darkorange')

document_swissse     = [data_swissse[x]['document'] for x in xs]
plt.scatter(xs, document_swissse, marker = 'x', c = 'darkorange')

plt.locator_params(axis='x', nbins=6)
plt.yscale('log')

plt.title('Storage', fontsize=16)
plt.xlabel('# documents', fontsize=14)
plt.ylabel('Size (MB)', fontsize=14)

plt.xticks(fontsize=14)
plt.yticks(fontsize=14)

plt.legend(labels = ('Plaintext (Index)', 'SWiSSSE (Index)', 'Plaintext (Document)', 'SWiSSSE (Document)'), fontsize=14)
plt.show()
