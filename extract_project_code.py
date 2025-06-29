import os
import chardet

project_roots = ["src"]
target_extensions = (".java", ".xml", ".yaml", ".yml", "Dockerfile", ".jsx", ".gradle")
output_file = "all_code_output.txt"

def detect_encoding(file_path):
    with open(file_path, 'rb') as f:
        raw_data = f.read()
    result = chardet.detect(raw_data)
    return result['encoding'] or 'utf-8'

with open(output_file, "w", encoding="utf-8") as out_file:
    for project_root in project_roots:
        print(f"\n==== Processing project: {project_root} ====")
        for root, dirs, files in os.walk(project_root):
            for file in files:
                if file.endswith(target_extensions) or file == "Dockerfile":
                    file_path = os.path.join(root, file)
                    print(f"Processing file: {file_path}")

                    try:
                        encoding = detect_encoding(file_path)
                        with open(file_path, "r", encoding=encoding, errors="replace") as current_file:
                            code = current_file.read()
                    except Exception as e:
                        print(f"? Failed to read {file_path}: {e}")
                        continue

                    out_file.write(f"\n\n// ======= {file_path} =======\n\n")
                    out_file.write(code)

    docker_compose_path = "docker-compose.yml"
    if os.path.exists(docker_compose_path):
        print(f"\n==== Processing file: {docker_compose_path} ====")
        try:
            encoding = detect_encoding(docker_compose_path)
            with open(docker_compose_path, "r", encoding=encoding, errors="replace") as docker_file:
                docker_code = docker_file.read()
            out_file.write(f"\n\n// ======= {docker_compose_path} =======\n\n")
            out_file.write(docker_code)
        except Exception as e:
            print(f"? Failed to read {docker_compose_path}: {e}")
    else:
        print(f"\n?? WARNING: {docker_compose_path} not found!")

print(f"\n? Done! All files saved to {output_file}")
