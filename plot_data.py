import matplotlib.pyplot as plt
import re
from functools import reduce

files = [
    'data/darp/results/2022-01-17_18:50_a5-48_insertion_0.txt',
    'data/darp/results/2022-01-17_18:50_a5-48_insertion_1.txt',
    'data/darp/results/2022-01-18_10:27_a5-48_insertion_2.txt',
    'data/darp/results/2022-01-18_10:27_a5-48_insertion_3.txt',

    'data/darp/results/2022-01-17_18:50_a13-144_insertion_0.txt',
    'data/darp/results/2022-01-17_18:50_a13-144_insertion_1.txt',
    'data/darp/results/2022-01-17_18:50_a13-144_insertion_2.txt',
    'data/darp/results/2022-01-17_18:50_a13-144_insertion_3.txt',
]

name_mapping = {
    'insertion_0': 'node + LNS node',
    'insertion_1': 'node + LNS request',
    'insertion_2': 'request + LNS node',
    'insertion_3': 'request + LNS request',
}

pattern_best_result = 'Time: \d+.\d+ \/ (\d+.\d+).*Best objective: (\d+.\d+)'
pattern_sol_found = 't = (\d+.\d+).*obj = (\d+.\d+)'
pattern_instances = '([?:ab](?:\d+)-(?:\d+))_(.+).txt'

solver_lns_ffpa = 'LNS-FFPA'
values_lns_ffpa = {
    'a3-24': (190.77, 190.02),
    'a4-36': (292.86, 291.71),
    'a5-48': (304.45, 303.03),
    'a6-72': (505.15, 494.91),
    'a7-72': (547.39, 542.83),
    'a8-108': (711.60, 696.51),
    'a9-96': (595.05, 588.80),
    'a10-144': (911.18, 891.98),
    'a11-120': (662.56, 653.57),
    'a13-144': (832.74, 816.79),

    'b3-24': (164.46, 164.46),
    'b4-36': (248.31, 248.21),
    'b5-48': (301.67, 299.27),
    'b6-72': (477.75, 469.73),
    'b7-72': (504.69, 494.01),
    'b8-108': (633.51, 620.54),
    'b9-96': (566.48, 557.61),
    'b10-144': (857.95, 838.65),
    'b11-120': (610.33, 602.19),
    'b13-144': (785.13, 771.69),
}
values_lns_ffpa_a = {k: v for k, v in values_lns_ffpa.items() if k[0] == 'a'}
values_lns_ffpa_b = {k: v for k, v in values_lns_ffpa.items() if k[0] == 'b'}


def mean(l):
    return sum(l) / len(l)


def best_run(l):
    return reduce(lambda x, y: x if x[-1][1] < y[-1][1] else y, l)


instances = {}  # dict of {instance_name: {solver_name: [results_list]}
max_run = {}  # dict of {instance_name: {solver_name: max_run_time}

for filename in files:
    solver = None
    results = []
    solutions_found = []  # tuple of (time (s), objective)
    search_obj = re.search(pattern_instances, filename)
    instance = search_obj.group(1)
    if instance not in instances:
        instances[instance] = {}
    if instance not in max_run:
        max_run[instance] = {}
    try:
        with open(filename) as file:
            for line in file.readlines():
                if solver is None:
                    solver = line.replace('\n', '')
                else:
                    if (search_obj := re.search(pattern_best_result, line)) is not None:
                        results.append(float(search_obj.group(2)))
                        max_run_time = float(search_obj.group(1))
                        solutions_found.append([])
                    elif (search_obj := re.search(pattern_sol_found, line)) is not None:
                        solutions_found[-1].append((float(search_obj.group(1)), float(search_obj.group(2))))
    except IOError:
        print(f'error when attempting to read {filename}')
    else:
        instances[instance][solver] = solutions_found
        max_run[instance][solver] = max_run_time
        result_mean = mean(results)
        result_min = min(results)
        first = min([s[0] for s in solutions_found], key=lambda x: x[0])
        print(f'instance {instance} solver {solver}: best result = {result_min:.3f} mean result = {result_mean:.3f}. '
              f'first sol = {first[1]:.3f} at {first[0]:.3f} [s]')


def plot_run(values, label, max_run_time):
    t = [i[0] for i in values]
    obj = [i[1] for i in values]
    t.append(max_run_time)
    obj.append(obj[-1])
    plt.plot(t, obj, label=label)


def hightlight_best_result(values: list, decimal='.2f', best=min) -> list[str]:
    """
    transform a list of values into a list of str, highlighting the best values with \textbf
    :param values: int / float values to compare
    :return: values transformed into string, the best value being highlighted
    """
    best_val = best(values)
    s = []
    for val in values:
        s.append(f"{val:{decimal}}" if val != best_val else f"\\textbf{{{val:{decimal}}}}")
    return s


def print_table_vs_lns_ffpa(solver: str, values: dict, instance_class=None):
    if instance_class is not None:
        assert instance_class in 'ab'
        values_ffpa = {k: v for k, v in values_lns_ffpa.items() if k[0] == instance_class}
    else:
        values_ffpa = values_lns_ffpa
    print_table(solver, values, solver_lns_ffpa, values_ffpa)


def print_table(solver1: str, values1: dict, solver2: str, values2: dict):
    """
    print LaTeX table of the results
    :param solver1 name of the first solver in the table
    :param values1 results for the first solver. Key are the name for the instances, values are tuple of (mean, best)
    :param solver2 name of the second solver in the table
    :param values2 results for the first solver. Key are the name for the instances, values are tuple of (mean, best)
    """
    common_row = set(values1.keys()).intersection(values2.keys())
    instance_class = next(iter(common_row))[0]
    average_format = "\\hline \\multicolumn{{2}}{{|c|}}{{\\textit{{Avg.}}}} & " \
                     "{:s} & {:s} & {:s} & {:s} \\\\ \n"
    row_format = "{:d} & {:d} & {:s} & {:s} & {:s} & {:s} \\\\ \n"
    header = "\\begin{tabular}{|c|c|c|c|c|c|} \n" \
             "\\hline \n" \
             "\\multicolumn{6}{|c|}{\\textbf{5 minutes run}} \\\\ \n" \
             "\\hline \n" \
             "\\multicolumn{2}{|c|}{\\textbf{class} $" + instance_class + "$} & " \
             "\\multicolumn{2}{|c|}{\\textbf{" + solver1 + "}} & " \
             "\\multicolumn{2}{|c|}{\\textbf{" + solver2 + "}} \\\\ \n" \
             "\\hline \n" + \
             "m & n & Mean & Best & Mean & Best \\\\ \n" \
             "\\hline \n"
    footer = "\\hline \n" \
             "\\end{tabular} \n"
    common_row = set(values1.keys()).intersection(values2.keys())
    rows_unordered = []
    means_mean = [[], []]
    best_mean = [[], []]
    for ist in common_row:
        m, n = ist.split('-')
        m = int(m[1:])
        n = int(n)
        means = [values1[ist][0], values2[ist][0]]
        best = [values1[ist][1], values2[ist][1]]
        # append for mean computation
        for i, val in enumerate(means):
            means_mean[i].append(val)
        for i, val in enumerate(best):
            best_mean[i].append(val)
        # highlight the best results
        means = hightlight_best_result(means)
        best = hightlight_best_result(best)
        row_val = []
        for i in zip(means, best):
            row_val.extend(i)
        rows_unordered.append(((m, n), row_format.format(m, n, *row_val)))
    rows = reduce(lambda x, y: x + y[1], sorted(rows_unordered), "")
    means_mean = hightlight_best_result(list(map(lambda i: mean(i), means_mean)))
    best_mean = hightlight_best_result(list(map(lambda i: mean(i), best_mean)))
    avg_cumul = []
    for i in zip(means_mean, best_mean):
        avg_cumul.extend(i)
    average = average_format.format(*avg_cumul)
    print(header + rows + average + footer)


print_table(solver_lns_ffpa, values_lns_ffpa_a, solver_lns_ffpa, values_lns_ffpa_a)
"""
for instance, solver_dic in instances.items():
    # plot best run
    plt.figure()
    for solver, results in solver_dic.items():
        plot_run(best_run(results), name_mapping.get(solver, solver), max_run[instance][solver])
    plt.legend()
    plt.xlabel('time [s]')
    plt.ylabel('objective')
    plt.title(instance)
    plt.tight_layout()
    plt.grid()
    plt.savefig(f'data/darp/plot/{instance}')
    # plot each run
    for solver, results in solver_dic.items():
        plt.figure()
        name = name_mapping.get(solver, solver)
        for result in results:
            plot_run(result, None, max_run[instance][solver])
        plt.xlabel('time [s]')
        plt.ylabel('objective')
        plt.title(f'{instance} {name} ({len(results)} runs)')
        plt.tight_layout()
        plt.grid()
        plt.savefig(f'data/darp/plot/{instance}_{name.replace("+ ", "").replace(" ", "_")}')
"""