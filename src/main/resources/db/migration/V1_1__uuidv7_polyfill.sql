CREATE OR REPLACE FUNCTION uuidv7() RETURNS uuid AS $$
DECLARE
  v_time timestamp with time zone := null;
  v_secs bigint := null;
  v_msec bigint := null;
  v_usec bigint := null;
  v_hex_time varchar := null;
  v_hex_a varchar := null;
  v_hex_b varchar := null;
  v_hex_c varchar := null;
  v_bytes bytea;
BEGIN
  v_time := clock_timestamp();
  v_secs := EXTRACT(EPOCH FROM v_time);
  v_msec := mod(EXTRACT(MILLISECONDS FROM v_time)::numeric, 1000::numeric)::bigint;
  v_usec := mod(EXTRACT(MICROSECONDS FROM v_time)::numeric, 1000::numeric)::bigint;
  v_hex_time := lpad(to_hex((v_secs * 1000) + v_msec), 12, '0');
  v_bytes := gen_random_bytes(10);
  v_hex_a := lpad(to_hex(get_byte(v_bytes, 0)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 1)), 2, '0');
  v_hex_b := lpad(to_hex(get_byte(v_bytes, 2)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 3)), 2, '0');
  v_hex_c := lpad(to_hex(get_byte(v_bytes, 4)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 5)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 6)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 7)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 8)), 2, '0') || lpad(to_hex(get_byte(v_bytes, 9)), 2, '0');
  
  RETURN (
    v_hex_time || '-' ||
    v_hex_a || '-7' ||
    substr(v_hex_b, 2, 3) || '-8' ||
    substr(v_hex_c, 2, 3) || '-' ||
    substr(v_hex_c, 5, 12)
  )::uuid;
END;
$$ LANGUAGE plpgsql;
