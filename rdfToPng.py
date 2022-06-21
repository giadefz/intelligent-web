from rdflib.tools.rdf2dot import rdf2dot
from rdflib import Graph
import io
import pydotplus
from IPython.display import display, Image

def rdfToPng():
    g = Graph()
    g.parse("tableaux.rdf")
    stream = io.StringIO()
    rdf2dot(g, stream, opts={display})
    pydot_graph = pydotplus.graph_from_dot_data(stream.getvalue())
    pydot_graph.write_png("tableaux.png")

if __name__ == '__main__':
    rdfToPng()