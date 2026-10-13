// Plays the animated mockups in this folder outside Claude Design.
// It renders the small subset of the canvas's component format they use:
// {{holes}} in text and attributes, <sc-if value>, <sc-for list as>, onClick handlers,
// and a Component class with state, setState, renderVals and componentDidMount.
(function () {
  const SVG = "http://www.w3.org/2000/svg";
  const WHOLE = /^\s*\{\{([^}]+)\}\}\s*$/;
  const ANY = /\{\{([^}]+)\}\}/g;

  function lookup(scope, path) {
    path = path.trim();
    if (path === "true") return true;
    if (path === "false") return false;
    if (/^-?\d/.test(path)) return Number(path);
    return path.split(".").reduce((o, k) => (o == null ? undefined : o[k]), scope);
  }

  function hole(value, scope) {
    const m = WHOLE.exec(value || "");
    return m ? lookup(scope, m[1]) : value;
  }

  function interpolate(text, scope) {
    return text.replace(ANY, (_, path) => {
      const v = lookup(scope, path);
      return v == null ? "" : String(v);
    });
  }

  function build(nodes, scope, out) {
    for (const node of nodes) {
      if (node.nodeType === Node.TEXT_NODE) {
        out.push(document.createTextNode(interpolate(node.data, scope)));
        continue;
      }
      if (node.nodeType !== Node.ELEMENT_NODE) continue;
      const tag = node.localName;
      if (tag === "sc-if") {
        if (hole(node.getAttribute("value"), scope)) build(node.childNodes, scope, out);
        continue;
      }
      if (tag === "sc-for") {
        const list = hole(node.getAttribute("list"), scope) || [];
        const as = node.getAttribute("as") || "item";
        list.forEach((item, i) => build(node.childNodes, Object.assign({}, scope, { [as]: item, $index: i }), out));
        continue;
      }
      const el = node.namespaceURI === SVG ? document.createElementNS(SVG, tag) : document.createElement(tag);
      for (const attr of node.attributes) {
        const name = attr.name;
        if (name.startsWith("hint-")) continue;
        if (name.startsWith("on")) {
          el[name.toLowerCase()] = hole(attr.value, scope) || null;
          continue;
        }
        const m = WHOLE.exec(attr.value);
        if (m) {
          const v = lookup(scope, m[1]);
          if (typeof v === "boolean") {
            if (v) el.setAttribute(name, "");
          } else if (v != null) {
            el.setAttribute(name, String(v));
          }
        } else {
          el.setAttribute(name, interpolate(attr.value, scope));
        }
      }
      const kids = [];
      build(node.childNodes, scope, kids);
      kids.forEach((k) => el.appendChild(k));
      out.push(el);
    }
  }

  // Updates the live tree in place, so CSS transitions run and hover state survives a render.
  function morph(live, fresh) {
    const current = Array.from(live.childNodes);
    const next = Array.from(fresh.childNodes);
    next.forEach((n, i) => {
      const cur = current[i];
      if (!cur) {
        live.appendChild(n);
      } else if (cur.nodeType !== n.nodeType || cur.nodeName !== n.nodeName) {
        live.replaceChild(n, cur);
      } else if (n.nodeType === Node.TEXT_NODE) {
        if (cur.data !== n.data) cur.data = n.data;
      } else {
        for (const a of Array.from(cur.attributes)) if (!n.hasAttribute(a.name)) cur.removeAttribute(a.name);
        for (const a of n.attributes) if (cur.getAttribute(a.name) !== a.value) cur.setAttribute(a.name, a.value);
        cur.onclick = n.onclick;
        morph(cur, n);
      }
    });
    for (let i = next.length; i < current.length; i++) live.removeChild(current[i]);
  }

  let schedule = () => {};

  class DCLogic {
    constructor(props) {
      this.props = props || {};
      this.state = {};
    }
    setState(patch) {
      Object.assign(this.state, patch);
      schedule();
    }
    forceUpdate() {
      schedule();
    }
  }

  // Props come from the mockup's defaults, then the URL (?speed=60&reducedMotion=1),
  // and reducedMotion also follows the OS setting.
  function mountDC(Component, defaults) {
    const params = new URLSearchParams(location.search);
    const props = Object.assign({}, defaults);
    for (const [k, v] of params) props[k] = v === "1" || v === "true" ? true : v === "0" || v === "false" ? false : Number(v);
    if (window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches) props.reducedMotion = true;

    const template = document.getElementById("dc");
    const root = document.getElementById("root");
    const component = new Component(props);
    let queued = false;
    function render() {
      queued = false;
      const fresh = document.createElement("div");
      const out = [];
      build(template.content.childNodes, component.renderVals(), out);
      out.forEach((n) => fresh.appendChild(n));
      morph(root, fresh);
    }
    schedule = () => {
      if (!queued) {
        queued = true;
        requestAnimationFrame(render);
      }
    };
    render();
    if (component.componentDidMount) component.componentDidMount();
  }

  window.DCLogic = DCLogic;
  window.mountDC = mountDC;
})();
