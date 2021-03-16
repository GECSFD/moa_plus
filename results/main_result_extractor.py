import pandas as pd
import os

jar_name = 'moa-2020.07.2-SNAPSHOT.jar'
jar_path = '../moa/target/'
dump_path = './'
dump_name = 'out.csv'
result_file_name = 'result.txt'

if not os.path.exists(f'{jar_path}{jar_name}'):
    print('Please compile MOA before runing this')
    print('Use: ')
    print('\tmvn clean package') 
    print('on root folder')

# task params
max_instances = '200000'
sample_frequency = '100000'

# generator params
n_classes = '2'
n_nominals = '5'
n_numericals = '5'

# model params
remove_chance = '0.1'
remove_classes = 'True'

remove_chances = ['0.1', '0.2', '0.3', '0.4', '0.5', '0.6']

open(result_file_name, 'w').close() # clean the results file before

for i in range(len(remove_chances)):
    print(f'Running {i}')

    dump_name = 'out'+str(i)+'.csv'
    remove_chance = remove_chances[i]

    open(f'{dump_path}{dump_name}', 'w').close() # clean the output file before running again

    os.system(f'java -cp {jar_path}{jar_name} moa.DoTask "' +
            f'EvaluatePrequential ' + # task choosen
            f'-l (trees.SSLHoeffdingAdaptiveTree -C {remove_chance} -R {remove_classes}) ' + # class of selected model to be evaluated
            f'-s (generators.RandomTreeGenerator -c {n_classes} -o {n_nominals} -u {n_numericals}) ' + # generator of the stream
            f'-i {max_instances} ' + # max instances to classify
            f'-d {dump_path}{dump_name} -f {sample_frequency}' + # dump options to dump
            f'"')

    csv_result = pd.read_csv(f'{dump_path}{dump_name}').tail(1) # picks only the last one

    with open('result.txt', 'a') as result_file:
        result_file.write(f'Running {i}, with remove_chance = {remove_chances[i]}:\n')
        result_file.write(f'\tnumber of instances: {csv_result["classified instances"].item()}\n')
        result_file.write(f'\tevaluation time (cpu seconds) {csv_result["evaluation time (cpu seconds)"].item()}\n')
        result_file.write(f'\tclassified corect: {csv_result["classifications correct (percent)"].item()}\n\n')

