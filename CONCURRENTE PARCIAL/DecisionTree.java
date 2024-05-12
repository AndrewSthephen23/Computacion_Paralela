import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Random;

public class DecisionTree {
    private Node rootNode;
    Random random = new Random();
    private static class Node {
        private String attribute;
        private Map<Integer, Node> branches;
        private int result;

        Node(String attribute) {
            this.attribute = attribute;
            branches = new HashMap<>();
        }

        void addBranch(int value, Node node) {
            branches.put(value, node);
        }
    }

    DecisionTree() {
        rootNode = new Node("sexo");

        Node hombreNode = new Node("edad");
        hombreNode.addBranch(1, new Node("mortalidad alta"));
        hombreNode.addBranch(0, new Node("peso"));

        Node mujerNode = new Node("edad");
        mujerNode.addBranch(1, new Node("peso"));
        mujerNode.addBranch(0, new Node("mortalidad baja"));

        Node pesoNode = new Node("peso");
        pesoNode.addBranch(1, new Node("mortalidad alta"));
        pesoNode.addBranch(0, new Node("altura"));

        Node alturaNode = new Node("altura");
        alturaNode.addBranch(1, new Node("mortalidad baja"));
        alturaNode.addBranch(0, new Node("resultado indefinido"));

        setResults(hombreNode, 1, 0);
        setResults(mujerNode, 0, 1);
        setResults(pesoNode, 1, 0);
        setResults(alturaNode, 0, 1);

        rootNode.addBranch(1, hombreNode);
        rootNode.addBranch(0, mujerNode);
        hombreNode.branches.get(0).addBranch(1, pesoNode);
        pesoNode.branches.get(0).addBranch(1, alturaNode);
    }

    private void setResults(Node node, int resultForTrue, int resultForFalse) {
        node.branches.get(resultForTrue).result = resultForTrue;
        node.branches.get(resultForFalse).result = resultForFalse;
    }

    public int predict(int sexo, int edad, int peso, int altura) {
        return predictRecursive(rootNode, sexo, edad, peso, altura);
    }
    
    private int predictRecursive(Node node, int sexo, int edad, int peso, int altura) {
        if (node == null) {
            return random.nextInt(2); // Manejo de caso no cubierto
        }
    
        if (node.result != 0) { // Nodo terminal
            return node.result;
        }
    
        int nextAttribute = getNextAttribute(node, sexo, edad, peso, altura);
        Node nextNode = node.branches.get(nextAttribute);
    
        return predictRecursive(nextNode, sexo, edad, peso, altura);
    }
    
    private int getNextAttribute(Node node, int sexo, int edad, int peso, int altura) {
        if (node.attribute.equals("sexo")) {
            return sexo;
        } else if (node.attribute.equals("edad")) {
            return edad >= 50 ? 1 : 0;
        } else if (node.attribute.equals("peso")) {
            return peso >= 80 ? 1 : 0;
        } else if (node.attribute.equals("altura")) {
            return altura >= 170 ? 1 : 0;
        } else {
            
            return random.nextInt(2); // Manejo de caso no cubierto
        }
    }
}