#!/usr/bin/env python3
"""Validate the official contract and compare HTTP operations to controller mappings."""
import pathlib
import re
import sys
import yaml
from openapi_spec_validator import validate_spec

ROOT = pathlib.Path(__file__).resolve().parents[2]
CONTRACT = ROOT / 'docs/openapi.yaml'
CONTROLLERS = ROOT / 'src/main/java/com/kairos/module'

class UniqueLoader(yaml.SafeLoader):
    pass

def unique_mapping(loader, node):
    result = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node)
        if key in result:
            raise ValueError(f'duplicate YAML key {key!r} at line {key_node.start_mark.line + 1}')
        result[key] = loader.construct_object(value_node)
    return result

UniqueLoader.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, unique_mapping)

try:
    spec = yaml.load(CONTRACT.read_text(), Loader=UniqueLoader)
    validate_spec(spec)
    documented = {(method.upper(), path) for path, item in spec['paths'].items()
                  for method in item if method in {'get', 'post', 'put', 'patch', 'delete'}}
    actual = set()
    for controller in CONTROLLERS.rglob('*Controller.java'):
        source = controller.read_text()
        base = re.search(r'@RequestMapping\("([^"]+)"\)', source)
        if not base:
            continue
        for method, route in re.findall(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', source):
            actual.add((method.upper(), base.group(1) + route))
    if actual != documented:
        raise ValueError(f'controller/contract mismatch; missing={sorted(actual-documented)}, extra={sorted(documented-actual)}')
    print(f'OpenAPI valid; {len(actual)} controller operations match the contract')
except Exception as error:
    print(f'OpenAPI validation failed: {error}', file=sys.stderr)
    sys.exit(1)
