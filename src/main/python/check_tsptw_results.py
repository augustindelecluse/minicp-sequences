# read every line of the found solutions and check them using the checker from https://lopez-ibanez.eu/tsptw-instances
import os
import subprocess
import re
import sys

sol_folder = "data/TSPTW/results/best_found"
instance_folder = "data/TSPTW/instances"
checker_path = "src/main/cpp/check_solution"
pattern_sol = "(.*)\W+\|\W.*\W\|\W.*\W\|\W+(\d+\.*\d*)\W+\|\W+\d+\.\d+\W+\|\W(.*)\W"
pattern_checker = "tourcost = (\d+\.*\d*).*constraint violations = (\d+)"

#instance_set = file_sol.split('/')[-1].split('_')[0]

for file in os.listdir(sol_folder):
    file_sol = f"{sol_folder}/{file}"
    if not os.path.isfile(file_sol):
        continue
    print(f'reading solutions from {file}')
    with open(file_sol) as sol_provider:
        for line in sol_provider:
            if line.startswith("#"):
                continue
            try:
                # retrieve the instance and the solution
                if (search_obj := re.search(pattern_sol, line)) is not None:
                    if "crashed" in line:
                        continue
                    instance = search_obj.group(1)
                    objective = search_obj.group(2)
                    ordering = search_obj.group(3)
                    temporary_file = f"tmp"
                    instance_path = f"{instance_folder}/{instance}"
                    # write the found solution in a temporary txt file
                    with open(temporary_file, 'w') as new_sol_found:
                        new_sol_found.write(ordering)
                    # call the checker
                    result = subprocess.run([f'{checker_path}', instance_path, temporary_file], stdout=subprocess.PIPE,
                                            stderr=subprocess.PIPE)
                    output = result.stdout
                    output_err = str(result.stderr)
                    if output_err is not None and output_err != "":
                        a = 0
                    # delete the temporary file
                    os.remove(temporary_file)
                    if (search_obj2 := re.search(pattern_checker, str(output))) is not None:
                        cost = search_obj2.group(1)
                        violation = int(search_obj2.group(2))
                        valid_sol = violation == 0
                        # some cost might be scaled and some not. convert to string, remove dot and remove zeros at the
                        # end to compare the results
                        cost_transformed = str(cost).replace('.', '')
                        while cost_transformed[-1] == '0':
                            cost_transformed = cost_transformed[:-1]
                        objective_transformed = str(objective).replace('.', '')
                        while objective_transformed[-1] == '0':
                            objective_transformed = objective_transformed[:-1]
                        same_objective = objective_transformed == cost_transformed
                        if same_objective:
                            detail = "same cost after scaling"
                        else:
                            detail = "different cost after scaling"
                        print(f"\tinstance {instance}: cost from checker {cost} vs {objective} (self computed). "
                              f"{'in' if not valid_sol else ''}valid solution. {detail}")
                        if not same_objective:
                            print(f"\t {detail} for {instance}", file=sys.stderr)

                        if not valid_sol:
                            print(f"\t invalid solution for {instance}", file=sys.stderr)
                    else:
                        print(f"failed to parse output for instance {instance}")
            except:
                pass
    print('\n')
