import os
import re

new_best_folder = "data/TSPTW/results/best_found"
old_best_folder = "data/TSPTW/best_known_sol"
pattern_objective = "improved.*\|\s+(\d+\.\d+)\s+\|\s+(\d+\.\d+)\s+\|"
pattern_old_objective = "([^\s]+)\W+(\d+\.*\d*)\W+"


def all_filename_in(folder: str) -> list[str]:
    return [f"{folder}/{file}" for file in os.listdir(folder) if os.path.isfile(f"{folder}/{file}")]


def all_new_best_files_for(instance_set: str) -> list[str]:
    return [f"{new_best_folder}/{file}" for file in os.listdir(new_best_folder)
            if os.path.isfile(f"{new_best_folder}/{file}") and instance_set in file]


def old_best_sol() -> dict[str, float]:
    old_best = {}
    for filename in all_filename_in(old_best_folder):
        with open(filename) as file:
            for line in file:
                if (search_obj := re.search(pattern_old_objective, line)) is not None:
                    old_best[search_obj.group(1)] = float(search_obj.group(2))
    return old_best


# give {instance: (best_sol, time)}
def new_best_sol_in_file(file: str) -> dict[str, tuple[float, float]]:
    new_best = {}
    with open(file) as f:
        for line in f:
            if "improved" in line:
                instance = line.split(" ")[0]
                if (search_obj := re.search(pattern_objective, line)) is not None:
                    new_best[instance] = float(search_obj.group(1)), float(search_obj.group(2))
    return new_best


instance_old_best = old_best_sol()
instance_new_best = {}
for file in all_filename_in(old_best_folder):
    instance_set = file.split("/")[-1].removesuffix(".txt")
    new_best_files = all_new_best_files_for(instance_set)
    for new_best_file in new_best_files:
        print(new_best_file)
        new_best_reported = new_best_sol_in_file(new_best_file)
        for instance, (obj, time) in new_best_reported.items():
            if instance not in instance_new_best or \
                    obj < instance_new_best[instance][0] or \
                    (obj == instance_new_best[instance][0] and time < instance_new_best[instance][1]):
                instance_new_best[instance] = obj, time
print(f"{len(instance_new_best)} improved instances")

table = ""
for instance, (obj, time) in instance_new_best.items():
    instance_set, instance_name = instance.split("/")
    tinier_name = ".".join(instance_name.split(".")[:-1])  # remove extension
    old_obj = instance_old_best[instance_name]
    obj = obj * 10
    if old_obj.is_integer() and obj.is_integer():
        old_obj = int(old_obj)
        obj = int(obj)
    tab_entry = f"{instance_set} & {tinier_name} & ${old_obj}$ & ${obj}$ & ${time:.2f}$ \\\\ \n"
    table += tab_entry
print(table)

