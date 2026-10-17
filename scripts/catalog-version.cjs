const fs = require('node:fs');
const path = require('node:path');
const content = fs.readFileSync(path.join(__dirname, '../native/gradle/libs.versions.toml'), 'utf8');
const versions = content.split('[versions]')[1].split(/^\[/m)[0];
const requested = process.argv[2];
if (!/^[a-z][a-z0-9-]*$/.test(requested || '')) throw new Error('A catalog version alias is required');
const value = versions.match(new RegExp(`^${requested}\\s*=\\s*"([^"]+)"`, 'm'))?.[1];
if (!value) throw new Error(`Missing catalog version: ${requested}`);
process.stdout.write(value);
