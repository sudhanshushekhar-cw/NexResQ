package com.example.nexresq;

public class Model {
    public static class Service{
        private String id;
        private String name;

        Service(String id, String name){
            this.id = id;
            this.name = name;
        }

        public String getId(){
            return id;
        }

        public String getName(){
            return name;
        }

        @Override
        public String toString() {
            return name; // Spinner will show only the name
        }
    }
}
