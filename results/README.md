# Result Extractor

Possibilita rodar com várias opções e bem automatizado.

### Instalação dos requisitos:

```
pip3 install -r requirements.txt
```

## Guia de Uso:

Para rodar usando parâmetros default basta usar o comando:

```bash
python3 result_extractor.py
```

Parâmetros possíveis:

```
  -h, --help            show this help message and exit
  --max_instances MAX_INSTANCES, -m MAX_INSTANCES
                        maximo de instancias por modelo. Default: 200000
  --sample_frequency SAMPLE_FREQUENCY, -s SAMPLE_FREQUENCY
                        frequencia de retirada de informacoes. Default: 100000
  --n_classes N_CLASSES, -n N_CLASSES
                        numero de classes no stream. default=2
  --n_nominals N_NOMINALS, -nm N_NOMINALS
                        numero de classes nominais
  --n_numericals N_NUMERICALS, -nr N_NUMERICALS
                        numero de classes numericas. default=5
  --arff_file ARFF_FILE, -a ARFF_FILE
                        arff file to use in the stream generator, se for passado -n -nm -nr serão ignorados. default=None
  --remove_chance REMOVE_CHANCE, -r REMOVE_CHANCE
                        chance de remocao. default=0.1
  --remove_classes REMOVE_CLASSES, -rc REMOVE_CLASSES
                        True para remover aleatoriamente algumas classes e False para não. default=True
  --levatic_weight LEVATIC_WEIGHT, -W LEVATIC_WEIGHT
                        levatic weight. default=0.5
  --remove_chance_min REMOVE_CHANCE_MIN, -rmin REMOVE_CHANCE_MIN
                        Minimo de chance para o remove chances. default=0
  --remove_chance_max REMOVE_CHANCE_MAX, -rmax REMOVE_CHANCE_MAX
                        Máximo de chance para o remove chances. default=1
  --remove_chance_inc REMOVE_CHANCE_INC, -rinc REMOVE_CHANCE_INC
                        Incremento do remove chances. default=0.1
```
