import pandas as pd
import os

from argument_parser import parser

jar_name = 'moa-2020.07.2-SNAPSHOT.jar'
jar_path = '../moa/target/'
dump_path = './outs/'
dump_name = 'out.csv'
result_file_name = 'result.txt'

if not os.path.exists(dump_path):
    os.makedirs(dump_path)

if not os.path.exists(f'{jar_path}{jar_name}'):
    print('Please compile MOA before runing this')
    print('Use: ')
    print('\tmvn clean package') 
    print('on project root folder')

remove_chances = ['0.1']

if __name__ == '__main__':
    args = parser.parse_args()

    remove_chance = args.remove_chance
    remove_classes = args.remove_classes

    n_classes = args.n_classes
    n_nominals = args.n_nominals
    n_numericals = args.n_numericals

    arff_file = args.arff_file

    max_instances = args.max_instances
    sample_frequency = args.sample_frequency

    remove_chance_min = args.remove_chance_min
    remove_chance_max = args.remove_chance_max
    remove_chance_inc = args.remove_chance_inc
    assert(remove_chance_min <= remove_chance_max)

    levatic_weight = args.levatic_weight
    remove_chances = []
    remove_chance_item = remove_chance_min
    while remove_chance_item <= remove_chance_max:
        remove_chances.append(remove_chance_item)
        remove_chance_item = round(remove_chance_item+remove_chance_inc, 2)

    open(result_file_name, 'w').close() # clean the results file before

    stream_str = f'-s (generators.RandomTreeGenerator -c {n_classes} -o {n_nominals} -u {n_numericals}) ' # generator of the stream
    if not arff_file is None:
        stream_str = f'-s (ArffFileStream -f {arff_file})'

    nominal_class_observer = ''
    numeric_class_observer = ''
    if remove_classes:
        nominal_class_observer = '-d SSLNominalAttributeClassObserver'
        numeric_class_observer = '-n SSLGaussianNumericAttributeClassObserver'

    for i in range(len(remove_chances)):
        print(f'Running {i}')

        dump_name = 'out'+str(i)+'.csv'
        remove_chance = remove_chances[i]

        open(f'{dump_path}{dump_name}', 'w').close() # clean the output file before running again

        command = f'java -cp {jar_path}{jar_name} moa.DoTask "' \
                f'EvaluatePrequential ' \
                f'-l (trees.SSLHoeffdingAdaptiveTree {nominal_class_observer} {nominal_class_observer} -C {remove_chance} -R {remove_classes}) ' \
                f'{stream_str}' \
                f'-i {max_instances} ' \
                f'-d {dump_path}{dump_name} -f {sample_frequency}' \
                '"'

        print(f'Running with the command: {command}')

        os.system(command)

        csv_result = pd.read_csv(f'{dump_path}{dump_name}').tail(1) # picks only the last one

        with open('result.txt', 'a') as result_file:
            result_file.write(f'Running {i}, with remove_chance = {remove_chances[i]}:\n')
            result_file.write(f'\tnumber of instances: {csv_result["classified instances"].item()}\n')
            result_file.write(f'\tevaluation time (cpu seconds) {csv_result["evaluation time (cpu seconds)"].item()}\n')
            result_file.write(f'\tclassified corect: {csv_result["classifications correct (percent)"].item()}\n\n')

