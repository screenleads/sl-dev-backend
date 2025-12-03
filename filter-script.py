#!/usr/bin/env python3
import re

replacements = {
    b"52866617jJ@": b"***PASSWORD_REMOVED***",
    b"test_secret": b"***SECRET_REMOVED***",
    b"U0hKQkNGR0hJSktMTU5PUFFSU1RVVldYWVo3ODkwQUJDREVGRw==": b"***BASE64_REMOVED***",
    b"dummy_base64_value": b"***BASE64_REMOVED***",
}

def replace_secrets(blob, metadata):
    for old, new in replacements.items():
        blob.data = blob.data.replace(old, new)
    # Reemplazos con regex para claves parciales
    blob.data = re.sub(b'sk_test_[a-zA-Z0-9]+', b'sk_test_***REMOVED***', blob.data)
    blob.data = re.sub(b'rk_test_[a-zA-Z0-9]+', b'rk_test_***REMOVED***', blob.data)
    blob.data = re.sub(b'whsec_[a-zA-Z0-9]+', b'whsec_***REMOVED***', blob.data)
