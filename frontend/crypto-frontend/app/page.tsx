import Image from "next/image";
import {Suspense} from "react";
import ProductTable from "@/app/ProductTable";

async function fetchData() {
    await new Promise((resolve) => setTimeout(resolve, 1000));
    let products: Product[] = [
        {id: 1, name: 'Laptop', price: 1200, category: 'Electronics'},
        {id: 2, name: 'Keyboard', price: 75, category: 'Accessories'},
        {id: 3, name: 'Mouse', price: 25, category: 'Accessories'},
        {id: 4, name: 'Monitor', price: 300, category: 'Electronics'},
        {id: 5, name: 'Webcam', price: 50, category: 'Peripherals'},
    ]
    return {
        data: products
    }
}

export default async function Home() {
    const {data: products} = await fetchData();
    return (
        <main>
            <h1>Product List</h1>
            <Suspense fallback={<div>Loading table...</div>}>
                <ProductTable
                    initialProducts={products}
                />
            </Suspense>
        </main>
    );
}

export class Product {
    id: number;
    name: string;
    price: number;
    category: string;

    constructor(id: number, name: string, price: number, category: string) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.category = category;
    }
}