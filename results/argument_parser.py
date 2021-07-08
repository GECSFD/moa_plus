
import argparse

parser = argparse.ArgumentParser()

# task params
parser.add_argument(
    "--max_instances",
    "-m",
    type=int,
    default=200000,
    help="maximo de instancias por modelo"
)
parser.add_argument(
    "--sample_frequency",
    "-s",
    type=int,
    default=100000,
    help="frequencia de retirada de informacoes"
)

# stream generator params
parser.add_argument(
    "--n_classes",
    "-n",
    type=int,
    default=2,
    help="numero de classes no stream"
)
parser.add_argument(
    "--n_nominals",
    "-nm",
    type=int,
    default=5,
    help="numero de classes nominais"
)
parser.add_argument(
    "--n_numericals",
    "-nr",
    type=int,
    default=5,
    help="numero de classes numericas"
)

parser.add_argument(
    "--arff_file",
    "-a",
    type=str,
    default=None,
    help="arff file to use in the stream generator, if it is passed -n -nm -nr will be ignored"
)

# model params
parser.add_argument(
    "--remove_chance",
    "-r",
    type=float,
    default=0.1,
    help="chance de remocao"
)
parser.add_argument(
    "--remove_classes",
    "-rc",
    type=bool,
    default=True,
    help="True para remover aleatoriamente algumas classes e False para não"
)

# remove chances
parser.add_argument(
    "--remove_chance_min",
    "-rmin",
    type=float,
    default=0,
    help="Minimo de chance para o remove chances"
)
parser.add_argument(
    "--remove_chance_max",
    "-rmax",
    type=float,
    default=1,
    help="Máximo de chance para o remove chances"
)
parser.add_argument(
    "--remove_chance_inc",
    "-rinc",
    type=float,
    default=0.1,
    help="Incremento do remove chances"
)
